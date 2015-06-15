;; Copyright © 2015, JUXT LTD.

(ns yada.core
  (:require [bidi.bidi :refer (Matched succeed)]
            [bidi.ring :refer (Ring)]
            [cheshire.core :as json]
            [clojure.core.async :as a]
            [clojure.java.io :as io]
            [clojure.pprint :refer (pprint)]
            [clojure.set :as set]
            [clojure.string :as str]
            [clojure.tools.logging :refer :all :exclude [trace]]
            [clojure.walk :refer (keywordize-keys)]
            [hiccup.core :refer (html h)]
            [manifold.deferred :as d]
            [manifold.stream :refer (->source transform)]
            [ring.middleware.basic-authentication :as ba]
            [ring.middleware.params :refer (params-request)]
            [ring.swagger.coerce :as rc]
            [ring.swagger.schema :as rs]
            [ring.util.codec :refer (form-decode)]
            [ring.util.request :refer (character-encoding urlencoded-form? content-type)]
            [ring.util.time :refer (parse-date format-date)]
            [schema.coerce :refer (coercer string-coercion-matcher) :as sc]
            [schema.core :as s]
            [schema.utils :refer (error? error-val)]
            [yada.coerce :refer (coercion-matcher)]
            [yada.conneg :refer (best-allowed-content-type best-allowed-charset)]
            [yada.representation :as rep]
            [yada.resource-options :as ropts]
            [yada.resource :as yst]
            [yada.trace]
            [yada.util :refer (link)])
  (:import (clojure.lang IPending)
           (java.util Date)
           (java.util.concurrent Future)
           (manifold.deferred IDeferred Deferrable)
           (schema.utils ValidationError ErrorContainer)))

(def k-bidi-match-context :bidi/match-context)

;; TODO: Find a better name
(defprotocol YadaInvokeable
  (invoke-with-initial-context [_ req ctx]
    "Invoke the yada handler with some initial context entries"))

;; Yada returns instances of Endpoint, which allows yada to integrate
;; with bidi's matching-context.

;; Check duplication with yada/bidi ns
(defrecord Endpoint [resource handler]
  clojure.lang.IFn
  (invoke [_ req] (handler req {}))
  YadaInvokeable
  (invoke-with-initial-context [this req ctx] (handler req ctx))
  Matched
  (resolve-handler [this m]
    (succeed this m))
  (unresolve-handler [this m]
    (when (= this (:handler m)) ""))
  Ring
  (request [this req m]
    (handler req {k-bidi-match-context m})))

;; "It is better to have 100 functions operate on one data structure
;; than 10 functions on 10 data structures." — Alan Perlis

(defn- not* [[result m]]
  [(not result) m])

(defn spyctx [label & [korks]]
  (fn [ctx]
    (infof "SPYCTX %s: Context is %s"
            label
            (if korks (get-in ctx (if (sequential? korks) korks [korks])) ctx))
    ctx))

(defrecord NoAuthorizationSpecified []
  ropts/ResourceOptions
  (authorize [b ctx] true)
  (authorization [_] nil))

(def realms-xf
  (comp
   (filter (comp (partial = :basic) :type))
   (map :realm)))

(defn as-sequential [s]
  (if (sequential? s) s [s]))

(defn to-encoding [s encoding]
  (-> s
      (.getBytes)
      (java.io.ByteArrayInputStream.)
      (slurp :encoding encoding)))



;; Allowed methods

;; RFC 7231 - 8.1.3.  Registrations

;; | Method  | Safe | Idempotent | Reference     |
;; +---------+------+------------+---------------+
;; | CONNECT | no   | no         | Section 4.3.6 |
;; | DELETE  | no   | yes        | Section 4.3.5 |
;; | GET     | yes  | yes        | Section 4.3.1 |
;; | HEAD    | yes  | yes        | Section 4.3.2 |
;; | OPTIONS | yes  | yes        | Section 4.3.7 |
;; | POST    | no   | no         | Section 4.3.3 |
;; | PUT     | no   | yes        | Section 4.3.4 |
;; | TRACE   | yes  | yes        | Section 4.3.8 |


(def known-methods #{:connect :delete :get :head :options :post :put :trace})

(defn safe? [method]
  (contains? #{:get :head :options :trace} method))

(defn idempotent? [method]
  (contains? #{:delete :get :head :options :put :trace} method))

(defn allowed-methods [ctx]
  (let [rmap (:options ctx)]
    (if-let [methods (:methods rmap)]
      (set (ropts/allowed-methods methods))
      (set
       (remove nil?
               (conj
                (filter safe? known-methods)
                (when (:put rmap) :put)
                (when (:post rmap) :post)
                (when (:delete rmap) :delete)))))))

(defn make-endpoint
  [resource
   {:keys
    [service-available?                 ; async-supported
     known-method?                      ; async-supported
     request-uri-too-long?

     methods

     status                             ; async-supported
     headers                            ; async-supported

     ;; Security
     authorization                      ; async-supported
     security

     ;; Actions
     put!                               ; async-supported
     post!                              ; async-supported
     delete!                            ; async-supported
     patch!                             ; async-supported
     trace                              ; async-supported

     produces
     parameters

     ;; CORS
     allow-origin

     ] ;; :or {resource {}}

    :as options

    :or {authorization (NoAuthorizationSpecified.)}
    }]

  (let [security (as-sequential security)]

    (->Endpoint
     (yst/make-resource resource)
     (fn [req ctx]

       (let [method (:request-method req)
             debug (boolean (get-in req [:headers "x-yada-debug"]))]

         (-> ctx
             (merge
              {:request
               req
               ;; The next form is commented because we want to provide
               ;; access from individual State protocol implementations
               ;; to raw ByteBufers. You can only read the body stream
               ;; once, when it is required. Subsequent calls to :body
               ;; will yield nil. There's no way round this short of
               ;; temporarily storing the whole (potentially huge) body
               ;; somewhere.
               #_(assoc req
                        ;; We assoc in a 'delayed' body to the context's
                        ;; version of the request. The first caller to
                        ;; deref causes the slurp (which to do ahead of
                        ;; time might be unnecessarily wasteful).
                        :body (delay
                               (if-let [body (:body req)]
                                 ;; TODO Don't slurp, read into a byte array
                                 ;; Does aleph provide access to the underlying javax.nio.ByteBuffer(s)?
                                 ;; (yes - provided option raw-stream? is true)
                                 (slurp body :encoding (or (character-encoding req) "utf8"))
                                 nil)))
               :options options})

             (d/chain
              ;; TODO Inline these macros, they don't buy much for their complexity

              (link ctx
                (let [res (ropts/service-available? service-available? ctx)]
                  (if-not (ropts/interpret-service-available res)
                    (d/error-deferred
                     (ex-info "" (merge {:status 503
                                         ::http-response true}
                                        (when-let [retry-after (ropts/retry-after res)] {:headers {"retry-after" retry-after}})))))))

              (link ctx
                (when-not
                    (if known-method?
                      (ropts/known-method? known-method? method)
                      (contains? known-methods method)
                      )
                  (d/error-deferred (ex-info "" {:status 501
                                                 ::method method
                                                 ::http-response true}))))

              (link ctx
                (when (ropts/request-uri-too-long? request-uri-too-long? (:uri req))
                  (d/error-deferred (ex-info "" {:status 414
                                                 ::http-response true}))))

              ;; Method Not Allowed
              (link ctx
                (let [am (allowed-methods ctx)]
                  (when-not (contains? (allowed-methods ctx) method)
                    (d/error-deferred
                     (ex-info (format "Method not allowed: %s" method)
                              {:status 405
                               ::http-response true})))))

              ;; Malformed
              (fn [ctx]
                (let [keywordize (fn [m] (into {} (for [[k v] m] [(keyword k) v])))
                      parameters
                      {:path
                       (when-let [schema (get-in parameters [method :path])]
                         (rs/coerce schema (:route-params req) :query))

                       :query
                       (when-let [schema (get-in parameters [method :query])]
                         (rs/coerce schema (-> req params-request :query-params keywordize) :query))

                       :body
                       (when-let [schema (get-in parameters [method :body])]
                         (let [body (-> ctx :request :body deref)]
                           (rep/decode-representation body (content-type req) schema)))

                       :form
                       (when-let [schema (get-in parameters [method :form])]
                         (when (urlencoded-form? req)
                           (let [fp (keywordize-keys
                                     (form-decode (-> ctx :request :body deref)
                                                  (character-encoding req)))]
                             (rs/coerce schema fp :json))))

                       :header
                       (when-let [schema (get-in parameters [method :header])]
                         (let [params (select-keys (-> req :headers keywordize-keys) (keys schema))]
                           (rs/coerce schema params :query)))}]

                  (let [errors (filter (comp error? second) parameters)]

                    (if (not-empty errors)
                      (d/error-deferred (ex-info "" {:status 400
                                                     :body errors
                                                     ::http-response true}))
                      (let [body (:body parameters)
                            merged-params (merge (apply merge (vals (dissoc parameters :body)))
                                                 (when body {:body body}))]
                        (cond-> ctx
                          (not-empty merged-params) (assoc :parameters merged-params)))))))


              ;; Authentication
              (fn [ctx]
                (cond-> ctx
                  (not-empty (filter (comp (partial = :basic) :type) security))
                  (assoc :authentication (:basic-authentication (ba/basic-authentication-request req (fn [user password] {:user user :password password}))))))

              ;; Authorization
              (fn [ctx]
                (if-let [res (ropts/authorize authorization ctx)]
                  (if (= res :not-authorized)
                    (d/error-deferred
                     (ex-info ""
                              (merge
                               {:status 401 ::http-response true}
                               (when-let [basic-realm (first (sequence realms-xf security))]
                                 {:headers {"www-authenticate" (format "Basic realm=\"%s\"" basic-realm)}}))
                              ))
                    (if-let [auth (ropts/authorization res)]
                      (assoc ctx :authorization auth)
                      ctx))
                  (d/error-deferred (ex-info "" {:status 403
                                                 ::http-response true}))))

              ;; TRACE
              (link ctx
                (if (= method :trace)
                  (d/error-deferred
                   (ex-info "TRACE"
                            (merge
                             {::http-response true}
                             (if trace
                               ;; custom user-supplied implementation
                               (-> (merge
                                    {:status 200}
                                    (merge-with
                                     merge
                                     {:headers {"content-type" "message/http;charset=utf8"}}
                                     ;; Custom code /can/ override (but really shouldn't)
                                     (ropts/trace trace req ctx)))
                                   (update-in [:body] to-encoding "utf8"))

                               ;; otherwise default implementation
                               (let [body (-> ctx
                                              :request
                                              ;; A client MUST NOT generate header fields in a TRACE request containing sensitive
                                              ;; data that might be disclosed by the response. For example, it would be foolish for
                                              ;; a user agent to send stored user credentials [RFC7235] or cookies [RFC6265] in a
                                              ;; TRACE request.
                                              (update-in [:headers] dissoc "authorization" "cookie")
                                              yada.trace/print-request
                                              with-out-str
                                              ;; only "7bit", "8bit", or "binary" are permitted (RFC 7230 8.3.1)
                                              (to-encoding "utf8"))]

                                 {:status 200
                                  :headers {"content-type" "message/http;charset=utf8"
                                            "content-length" (.length body)}
                                  :body body
                                  ::http-response true})))))))

              ;; TODO Not implemented (if unknown Content-* header)

              ;; TODO Unsupported media type

              ;; TODO Request entity too large - shouldn't we do this later,
              ;; when we determine we actually need to read the request body?

              ;; TODO OPTIONS

              ;; Prior to conneg we do a pre-fetch, so the resource has
              ;; a chance to load any metadata it may need to answer the
              ;; questions to follow. It can also load state in this
              ;; step, if it wants, but can defer this to the get-state
              ;; call if it wants. This would usually depend on the size
              ;; of the state. For example, if it's a stream, it would
              ;; be returned on get-state.

              ;; The pre-fetch can return a deferred result. (TODO think
              ;; about this) ; yes it can because the implementation can
              ;; check it's deferred and then place it in a chain
              (fn [ctx]
                (d/chain
                 (yst/fetch resource ctx)
                 (fn [res]
                   (assoc ctx :resource res))))

              ;; Content-type and charset negotiation - done here to throw back to the client any errors
              (fn [ctx]
                (let [available-content-types
                      (remove nil?
                              (or (ropts/produces produces ctx)
                                  (yst/produces resource ctx)))]

                  (if-let [content-type
                           (best-allowed-content-type
                            (or (get-in req [:headers "accept"]) "*/*")
                            (map (comp rep/full-type rep/to-media-type-map) available-content-types))]

                    ;; A request without any Accept-Charset header field
                    ;; implies that the user agent will accept any
                    ;; charset in response.  Most general-purpose user
                    ;; agents do not send Accept-Charset.
                    ;; rfc7231.html#section-5.3.3
                    #_(when-let [accept-charset (get-in req [:headers "accept-charset"])]
                        (throw (ex-info "TODO" {}))
                      )

                    (assoc-in ctx [:response :content-type] content-type)

                    ;; No best allowed content type
                    (if (not-empty available-content-types)
                      ;; If there is a produces specification, but not
                      ;; matched content-type, it's a 406.
                      (d/error-deferred
                       (ex-info ""
                                {:status 406
                                 ::debug {:message "Debug!"
                                          :available-content-types available-content-types}
                                 ::http-response true}))
                      ;; Otherwise return the context unchanged
                      ctx))))

              (fn [ctx]
                (case method
                  (:get :head)
                  (d/chain
                   ctx

                   ;; Perhaps the resource doesn't exist
                   (link ctx
                     (when-let [resource (:resource ctx)]
                       (when-not (yst/exists? resource ctx)
                         (d/error-deferred (ex-info "" {:status 404
                                                        ::http-response true})))))


                   ;; Conditional request
                   (link ctx
                     (when-let [last-modified (yst/last-modified (:resource ctx) ctx)]

                       (if-let [if-modified-since (some-> req
                                                          (get-in [:headers "if-modified-since"])
                                                          parse-date)]
                         ;; TODO: Hang on, we can't deref in the
                         ;; middle of a handler like this, we need to
                         ;; build a chain (I think)
                         (let [last-modified
                               (if (d/deferrable? last-modified) @last-modified last-modified)]

                           (if (<=
                                (.getTime last-modified)
                                (.getTime if-modified-since))

                             ;; exit with 304
                             (d/error-deferred
                              (ex-info "" (merge {:status 304
                                                  ::http-response true}
                                                 ctx)))

                             (assoc-in ctx [:response :headers "last-modified"] (format-date last-modified))))

                         (or
                          ;; TODO: Hang on, we can't deref in the
                          ;; middle of a handler like this, we need to
                          ;; build a chain (I think)

                          ;; TODO: Also, haven't we already calculated last-modified?

                          (some->> (if (d/deferrable? last-modified) @last-modified last-modified)
                                   format-date
                                   (assoc-in ctx [:response :headers "last-modified"]))
                          ctx))))

                   ;; Get body
                   (fn [ctx]
                     (let [content-type (get-in ctx [:response :content-type])]

                       (case method
                         :head (if content-type
                                 ;; We don't need to add Content-Length,
                                 ;; Content-Range, Trailer or Tranfer-Encoding, as
                                 ;; per rfc7231.html#section-3.3
                                 (update-in ctx [:response :headers] assoc "content-type" content-type)
                                 ctx)

                         :get
                         (d/chain

                          ;; the resource here can still be deferred
                          (:resource ctx)

                          (fn [resource]
                            (or (yst/get-state resource content-type ctx)
                                (throw (ex-info "" {:status 404
                                                    ;; TODO: Do something nice for developers here
                                                    :body "Not Found"
                                                    ::http-response true}))))

                          (fn [body]
                            (assoc-in ctx [:response :body] body))

                          (link ctx
                            (when content-type
                              (update-in ctx [:response :headers] assoc "content-type" content-type)))

                          (link ctx
                            (when-let [content-length (yst/content-length (:resource ctx) ctx)]
                              (update-in ctx [:response :headers] assoc "content-length" content-length))))))))


                  :put
                  (d/chain
                   ctx
                   (link ctx
                     (when-let [etag (get-in req [:headers "if-match"])]
                       (when (not= etag (get-in ctx [:resource :etag]))
                         (throw
                          (ex-info "Precondition failed"
                                   {:status 412
                                    ::http-response true})))))

                   ;; Do the put!
                   (fn [ctx]
                     (let [exists? (yst/exists? resource ctx)]
                       (let [res
                             (cond
                               put! (ropts/put! put! ctx)
                               ;; TODO: Add content and content-type
                               resource (yst/put-state! resource nil nil ctx)
                               :otherwise (throw (ex-info "No implementation of put!" {})))]

                         (assoc-in ctx [:response :status]
                                   (cond
                                     (d/deferred? res) 202
                                     exists? 204
                                     :otherwise 201))))))

                  :post
                  (d/chain
                   ctx

                   (link ctx
                     (when-let [etag (get-in req [:headers "if-match"])]
                       (when (not= etag (get-in ctx [:resource :etag]))
                         (throw
                          (ex-info "Precondition failed"
                                   {:status 412
                                    ::http-response true})))))

                   (fn [ctx]
                     (let [result
                           (cond
                             post! (ropts/post! post! ctx)
                             resource (yst/post-state! resource ctx)
                             :otherwise (throw (ex-info "No implementation of put!" {})))]

                       ;; TODO: what if error?

                       (assoc ctx :post-result result)

                       ))

                   (fn [ctx]
                     (ropts/interpret-post-result (:post-result ctx) ctx))


                   (fn [ctx]
                     (-> ctx
                         (assoc-in [:response :status] 200))))

                  :delete
                  (d/chain
                   ctx
                   (fn [ctx]
                     (if-not (yst/exists? resource ctx)
                       (assoc-in ctx [:response :status] 404)
                       (let [res
                             (cond
                               delete! (ropts/delete! delete! ctx)
                               resource (yst/delete-state! resource ctx)
                               :otherwise (throw (ex-info "No implementation of delete!" {})))]
                         (assoc-in ctx [:response :status] (if (d/deferred? res) 202 204))))))

                  :options
                  (d/chain
                   ctx
                   (link ctx
                     (if-let [origin (ropts/allow-origin allow-origin ctx)]
                       (update-in ctx [:response :headers]
                                  merge {"access-control-allow-origin"
                                         origin
                                         "access-control-allow-methods"
                                         (apply str
                                                (interpose ", " ["GET" "POST" "PUT" "DELETE"]))}))))

                  (throw (ex-info "Unimplemented method"
                                  {:status 501
                                   ::method method
                                   ::http-response true})))

                #_(cond
                    ;; 'Exists' flow
                    ;; TODO: Not sure that exists-flow is what we're doing here - exists-flow has all kinds of things complected in it. Break up into individual cases, perhaps based on method. We are only using this to avoid the 404 of the 'not exists' flow. Perhaps check for existence and throw the 404 now if necessary.
                    (or resource body (#{:post :put} method))
                    (d/chain
                     ctx
                     ;; Not sure we should use exists-flow for CORS pre-flight requests, should handle further above
                     (exists-flow method resource req status headers body post! allow-origin)
                     (cors allow-origin)

                     (fn [ctx]
                       (merge
                        {:status (or (get-in ctx [:response :status])
                                     (ropts/status status ctx)
                                     200)
                         :headers (merge
                                   (get-in ctx [:response :headers])
                                   (ropts/headers headers ctx))
                         ;; TODO :status and :headers should be implemented like this in all cases
                         :body (get-in ctx [:response :body])})))

                    ;; 'Not exists' flow
                    :otherwise
                    (d/chain ctx (constantly {:status 404}))))

              #_(fn [ctx]
                  (if-let [origin (ropts/allow-origin allow-origin ctx)]
                    (update-in ctx [:response :headers]
                               merge {"access-control-allow-origin"
                                      origin
                                      "access-control-expose-headers"
                                      (apply str
                                             (interpose ", " ["Server" "Date" "Content-Length" "Access-Control-Allow-Origin"]))})
                    ctx))

              ;; Response generation
              (fn [ctx]
                (let [response
                      (merge
                       {:status (or (get-in ctx [:response :status])
                                    (ropts/status status ctx)
                                    200)
                        :headers (merge
                                  (get-in ctx [:response :headers])
                                  (ropts/headers headers ctx))

                        ;; TODO :status and :headers should be implemented like this in all cases
                        :body (get-in ctx [:response :body])})]
                  (debugf "Returning response: %s" (dissoc response :body))
                  response
                  ))

              ;; End of chain
              )

             ;; Handle exits
             (d/catch clojure.lang.ExceptionInfo
                 #(let [data (ex-data %)]
                    (if (::http-response data)
                      (if-let [debug-data (when debug (::debug data))]
                        (assoc data :body (prn-str debug-data))
                        data)

                      (throw (ex-info "Internal Server Error (ex-info)" data %))
                      #_{:status 500
                         :body (format "Internal Server Error: %s" (pr-str data))})))

             (d/catch #(identity
                        (throw (ex-info "Internal Server Error" {:request req} %))
                        #_{:status 500 :body
                           (html
                            [:body
                             [:h1 "Internal Server Error"]
                             [:p (str %)]
                             [:pre (with-out-str (apply str (interpose "\n" (seq (.getStackTrace %)))))]])}))))))))

(defn yada
  ([resource]
   (yada resource {}))
  ([resource opts]
   (if (keyword? resource)
     (throw (ex-info "Deprecated usage" {}))
     (make-endpoint resource opts))))
