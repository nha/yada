;; Copyright © 2014-2016, JUXT LTD.

(ns yada.context
  (:require
   [clojure.spec :as s]
   clojure.string
   yada.resource
   yada.response
   yada.profile
   [ring.core.spec :as rs]))

(defrecord
    ^{:doc "This record represents the request context, which
    contains per-request information and is threaded through each of
    the interceptors."}
    Context [])

(s/def :yada/context
  (s/keys :req [:ring/request
                :yada/method-token
                :yada/profile
                :yada/handler]))

(defn method-token [ctx]
  (-> ctx :yada/method-token))

(defn context [init-context]
  (let [input-spec (s/keys :req [:ring/request])]
    (when-not (s/valid? input-spec init-context)
      (throw
       (ex-info
        (format "Initial context is not valid: %s"
                (s/explain-str input-spec init-context))
        {:init-context init-context :explain (s/explain-data input-spec init-context)}))))

  (let [context
        (assoc init-context
               :yada/method-token (-> init-context :ring/request :request-method name clojure.string/upper-case)
               :yada/response (yada.response/new-response)
               :yada/profile :dev)]
    (when-not (s/valid? :yada/context context)
      (throw
       (ex-info
        (format "Context is not valid: %s"
                (s/explain-str :yada/context context))
        {:context context :explain (s/explain-data :yada/context context)})))
    (map->Context context)))

(defn lookup-method [ctx]
  (yada.resource/lookup-method
   (:yada/resource ctx)
   (:yada/method-token ctx)))
