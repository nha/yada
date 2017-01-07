;; Copyright © 2016, JUXT LTD.

(ns yada.oauth
  (:require
   [aleph.http :as http]
   [buddy.core.hash :as hash]
   [buddy.core.keys :as keys]
   [buddy.sign.jwt :as jwt]
   [byte-streams :as b]
   [cheshire.core :as json]
   [clojure.string :as str]
   [clojure.tools.logging :refer :all]
   [clj-time.core :as time]
   [clojure.java.io :as io]
   [hiccup.core :refer [html h]]
   [manifold.deferred :as d]
   [ring.util.codec :as codec]
   [ring.util.response :as response]
   [schema.core :as s]
   [yada.body :refer [render-error]]
   [yada.yada :refer [resource uri-info]]
   [yada.cookies :refer [CookieValue]]
   [yada.security :refer [verify]]))

;; http://ncona.com/2015/02/consuming-a-google-id-token-from-a-server/

(s/defn oauth2-initiate-resource
  "Returns a resource that can be used in a GET or a POST which
  redirects to the OAuth2 authentication server to initiate the
  acquisition of an access-token for the user."
  [opts :- {:type (s/enum :github :google)
            (s/optional-key :id) s/Keyword
            :client-id s/Str
            ;; Where to send the user after authorization, use a keyword here for yada's uri-for function
            :redirect-uri s/Keyword
            :scope s/Str
            :secret (Class/forName "[B")
            :authorization-uri s/Str
            ;; The default target URI to redirect to on successful
            ;; authentication, can be overridden via full URIs passed
            ;; as query or form parameters
            (s/optional-key :target-uri) s/Keyword
            }]
  (let [initiate (fn [ctx
                      {:keys [client-id redirect-uri scope secret authorization-uri target-uri type]}
                      target-uri-override]
                   (merge
                    (:response ctx)
                    (response/redirect
                     (str authorization-uri "?"
                          (codec/form-encode
                           (merge
                            {"client_id" client-id
                             "redirect_uri" (:uri (uri-info ctx redirect-uri))
                             "scope" scope
                             "state" (jwt/encrypt {:target-uri (or target-uri-override
                                                                   (when target-uri (:uri (uri-info ctx target-uri)))
                                                                   "")}
                                                  secret)}
                            (when (= type :google) {"response_type" "code"})))))))]
    (resource
     (merge
      (when-let [id (:id opts)] {:id id})
      {:methods
       {:get {:produces "text/plain" ; it's a redirect, but we need to allow content neg to succeed
              :parameters {:query {(s/optional-key :target-uri) s/Str}}
              :response (fn [ctx] (initiate ctx opts (-> ctx :parameters :query :target-uri)))}
        :post {:consumes "application/x-www-form-urlencoded"
               :parameters {:form {(s/optional-key :target-uri) s/Str}}
               :response (fn [ctx] (initiate ctx opts (-> ctx :parameters :form :target-uri)))}}}))))


(s/defschema ^:private CookieOptions (merge
                                      {(s/optional-key :expiry-period) org.joda.time.Period}
                                      (select-keys CookieValue [;;(s/optional-key :expires)
                                                                (s/optional-key :max-age)
                                                                (s/optional-key :domain)
                                                                (s/optional-key :path)
                                                                (s/optional-key :secure)])))

(defn- session-cookie [cookie data secret]
  (let [expires (or ;; (:expires cookie) ;; TODO either expires (string or date?) or expiry-period (joda time period) in the schema
                 (time/plus (time/now) (or (:expiry-period cookie) (time/days 30))))]
    (merge (dissoc cookie :expiry-period)
           {:value (jwt/encrypt data secret)
            :expires expires
            :http-only true})))

(s/defn oauth2-callback-resource-github
  [opts :- {(s/optional-key :id) s/Keyword
            :client-id s/Str
            :client-secret s/Str
            :secret (Class/forName "[B")
            ;; The function that will ultimately call the third-party API for user-details.
            ;; First argument is the access-token
            :access-token-handler (s/=> {s/Any s/Any} s/Str)
            :access-token-url s/Str
            (s/optional-key :cookie) CookieOptions}]

  (let [{:keys [client-id client-secret secret access-token-handler access-token-url cookie]} opts]
    (assert access-token-handler)
    (resource
     (merge
      (when-let [id (:id opts)]
        {:id id})
      {:methods
       {:get
        {:produces "text/html"
         :parameters {:query {(s/optional-key :code) s/Str
                              (s/optional-key :state) s/Str
                              (s/optional-key :error) s/Str
                              (s/optional-key "error_description") s/Str
                              (s/optional-key "error_uri") s/Str}}
         :response
         (fn [ctx]

           (if-let [error (-> ctx :parameters :query :error)]
             (str "ERROR: " (-> ctx :parameters :query (get "error_description")))

             (let [code (-> ctx :parameters :query :code)
                   state (jwt/decrypt (-> ctx :parameters :query :state) secret)
                   target-uri (:target-uri state)]

               ;; Make API calls to GitHub without blocking the request thread
               (d/chain

                ;; Using the code, try to acquire an access token for API.
                ;; Note we are using an async client HTTP API here (aleph)
                (http/post
                 access-token-url
                 {:accept "application/json"
                  :form-params {"client_id" client-id
                                "client_secret" client-secret
                                "code" code}})

                (fn [response]
                  (if-not (= (:status response) 200)
                    (d/error-deferred (ex-info "Didn't get 200 from access_token call" {}))
                    (json/parse-stream (io/reader (:body response)))))

                (fn [json]
                  (if (get json "error")
                    (d/error-deferred (ex-info "access_token call returned error" json))
                    (get json "access_token")))

                (fn [access-token]
                  (if-not access-token
                    (d/error-deferred (ex-info "No access token in response" {}))
                    (access-token-handler access-token)))

                (fn [data]
                  (if (nil? data)
                    (d/error-deferred (ex-info "Forbidden" {:status 403}))

                    ;; TODO: Refresh tokens
                    (merge (:response ctx)
                           {:cookies {"session" (session-cookie cookie data secret)}}
                           (response/redirect target-uri))))))))}}}))))

(s/defn oauth2-callback-resource-google
  [opts :- {(s/optional-key :id) s/Keyword
            :access-token-url s/Str
            :client-id s/Str
            :client-secret s/Str
            :secret (Class/forName "[B")
            :redirect-uri s/Keyword

            ;; The function that will ultimately call the third-party API for user-details.
            ;; First argument is the access-token
            :handler (s/=> {s/Any s/Any} {:access-token s/Str :openid-claims {s/Str s/Str}})
            (s/optional-key :cookie) CookieOptions
            }]

  (let [{:keys [access-token-url client-id client-secret secret redirect-uri handler cookie]} opts]
    (assert handler)
    (resource
     (merge
      (when-let [id (:id opts)]
        {:id id})
      {:methods
       {:get
        {:produces "text/html"
         :parameters
         {:query
          {:authuser s/Str
           (s/optional-key :hd) s/Str
           (s/required-key "session_state") s/Str
           :prompt s/Str
           :state s/Str
           :code s/Str}}
         :response
         (fn [ctx]
           (let [state (jwt/decrypt (-> ctx :parameters :query :state) secret)
                 target-uri (:target-uri state)]

             ;; Make API calls to Google without blocking the request thread
             (d/chain
              (http/post
               access-token-url
               {:accept "application/json"
                :form-params {"code" (-> ctx :parameters :query :code)
                              "client_id" client-id
                              "client_secret" client-secret
                              "redirect_uri" (:uri (uri-info ctx redirect-uri))
                              "grant_type" "authorization_code"}
                :throw-exceptions false})

              (fn [response]
                (if-not (= (:status response) 200)
                  (d/error-deferred (ex-info "Didn't get 200 from access_token call" {:response (update response :body b/to-string)}))
                  (json/parse-string (b/to-string (:body response)))))

              (fn [json]
                (let [access-token (get json "access_token")
                      id-token (get json "id_token")
                      [header body sig] (map (comp b/to-string codec/base64-decode) (str/split id-token #"\."))
                      ]

                  ;; TODO: Verify sig (but this is deemed safe because of https)

                  {:access-token access-token
                   :openid-claims (json/parse-string body)}))

              handler

              (fn [data]
                  (if (nil? data)
                    (d/error-deferred (ex-info "Forbidden" {:status 403}))

                    ;; TODO: Refresh tokens
                    (merge (:response ctx)
                           {:cookies {"session" (session-cookie cookie data secret)}}
                           (response/redirect target-uri)))))))}}

       ;; If you don't want this behavior, replace the :responses
       ;; value in the resource with your own.
       :responses {500 {:produces #{"text/html" "text/plain"}
                        :response (fn [ctx]
                                    (let [error (:error ctx)]
                                      (cond
                                        (not (instance? clojure.lang.ExceptionInfo error))
                                        (render-error 500 error (-> ctx :response :produces) ctx)

                                        :otherwise
                                        (render-error 500 error (-> ctx :response :produces) ctx)
                                        #_(str "An error occured:" (pr-str error))
                                        )

                                      ))}}}))))

(defmethod verify :oauth2
  [ctx {:keys [cookie yada.oauth2/secret] :or {cookie "session"} :as scheme}]
  (when-not secret (throw (ex-info "Buddy JWT decryption requires a secret entry in scheme" {:scheme scheme})))
  (try
    (when-let [cookie (get-in ctx [:cookies cookie])]
      (jwt/decrypt cookie secret))
    (catch clojure.lang.ExceptionInfo e
      (when-not (contains? #{{:type :validation :cause :decryption}
                             {:type :validation :cause :signature}}
                           (ex-data e))
        (throw e)))))
