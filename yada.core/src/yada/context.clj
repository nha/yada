;; Copyright © 2014-2016, JUXT LTD.

(ns yada.context
  (:require
   [clojure.string :as str]
   [clojure.spec :as s]
   clojure.string
   yada.resource
   [ring.core.spec :as rs]))

;; Differs slightly from :ring/response in that status is not
;; mandatory.
(s/def :yada/response (s/keys :req [:ring.response/headers]
                              :opt [:ring.response/status
                                    :ring.response/body]))

(defrecord
    ^{:doc "This record is used as an escape mechanism users to
    return it in method responses. Doing this indicates to yada that
    the user knows what they are doing and want fine grained control
    over the response, perhaps setting cookies, response headers and
    so on."}
    Response [])

(defn response []
  (map->Response {:ring.response/headers {}}))

(defn ->ring-response [ctx]
  (let [response (:yada/response ctx)]
    (merge
     {:status (or (:ring.response/status response) 500)
      :headers (or (:ring.response/headers response) {})}
     (when-let [body (:ring.response/body response)]
       {:body body}))))

(defn add-status [ctx status]
  (assoc-in ctx [:yada/response :ring.response/status] status))

(defn add-body [ctx body]
  (assoc-in ctx [:yada/response :ring.response/body] body))

(defn add-header [ctx n v]
  (assoc-in ctx [:yada/response :ring.response/headers n] v))

(s/def :yada/context
  (s/keys :req [:yada/resource

                :ring/request
                ;; This would be ring/response but we like to be able
                ;; to return the response as a record for escape
                ;; hatches
                :yada/response

                :yada/method-token
                :yada/profile]))

(defn context "Create a request context"
  [init-ctx req]
  (merge
   {:ring/request req
    :yada/response (response)
    :yada/method-token (-> req :request-method name str/upper-case)}
   init-ctx))

(defn method-token [ctx]
  (-> ctx :yada/method-token))

(defn lookup-method [ctx]
  (yada.resource/lookup-method
   (:yada/resource ctx)
   (:yada/method-token ctx)))

(defn authentication-schemes [ctx]
  (-> ctx :yada/resource :yada.resource/authentication-schemes))
