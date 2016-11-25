;; Copyright © 2014-2016, JUXT LTD.

(ns yada.context
  (:require
   [clojure.string :as str]
   [clojure.spec :as s]
   clojure.string
   yada.resource
   [yada.profile :as p]
   [ring.core.spec :as rs]
   [yada.spec :refer [validate]]
   yada.cookies
   [yada.response :refer [new-response]]
   [ring.middleware.cookies :refer [cookies-request cookies-response]])
  (:import [yada.response Response]))

(s/def :yada/request (s/keys :req []))

(defn ->ring-response [ctx]
  (let [response (:yada/response ctx)]
    (merge
     {:status (or (:ring.response/status response) 500)
      :headers (merge (when-let [cookies (:yada.response/cookies response)]
                        (yada.cookies/->headers cookies))
                      (or (:ring.response/headers response) {}))}
     (when-let [body (:ring.response/body response)]
       {:body body})
     )))

(defn add-status [ctx status]
  (assoc-in ctx [:yada/response :ring.response/status] status))

(defn add-body [ctx body]
  (assoc-in ctx [:yada/response :ring.response/body] body))

(defn add-header [ctx n v]
  (assoc-in ctx [:yada/response :ring.response/headers n] v))

(s/def :yada/context
  (s/keys :req [:yada/resource
                :ring/request ; original Ring request
                :yada/request ; request extras, like cookies
                :yada/response
                :yada/method-token
                :yada/profile]))

(defn context "Create a request context"
  [init-ctx req]
  (merge
   {:ring/request req
    :yada/response (new-response)
    :yada/method-token (-> req :request-method name str/upper-case)
    :yada/request {:yada.request/cookies* (delay (:cookies (cookies-request req)))}}
   init-ctx))

(defn method-token [ctx]
  (-> ctx :yada/method-token))

(defn lookup-method [ctx]
  (yada.resource/lookup-method
   (:yada/resource ctx)
   (:yada/method-token ctx)))

(defn authentication-schemes [ctx]
  (-> ctx :yada/resource :yada.resource/authentication-schemes))

(defn response [ctx]
  (:yada/response ctx))

(defn set-status [response status]
  (assert (instance? Response response) "Response parameter is not a response record")
  (assoc-in response [:ring.response/status] status))

(defn set-body [response body]
  (assert (instance? Response response) "Response parameter is not a response record")
  (assoc-in response [:ring.response/body] body))
