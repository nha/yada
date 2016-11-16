;; Copyright © 2014-2016, JUXT LTD.

(ns yada.handler
  (:require [clojure.spec :as s]
            [clojure.spec.gen :as gen]
            [manifold.deferred :as d]
            [yada.method :refer [perform-method]]
            [yada.context :as ctx]
            yada.response))

(s/def :yada.handler/interceptor
  (s/with-gen fn? #(gen/return identity)))

(s/def :yada.handler/interceptor-chain
  (s/coll-of :yada.handler/interceptor))

(s/def :yada/handler
  (s/keys :req [:yada.handler/interceptor-chain]
          :opt [:yada.handler/error-interceptor-chain]))

(defn ^:interceptor terminate [ctx]
  (yada.response/->ring-response (:yada/response ctx)))

(defn apply-interceptors [ctx]
  (let [chain (-> ctx :yada/handler :yada.handler/interceptor-chain)]
    (->
     (apply d/chain ctx chain)
     (d/catch Exception
         (fn [e]
           (let [error-data (when (instance? clojure.lang.ExceptionInfo e) (ex-data e))
                 chain (or (get-in ctx [:yada/handler :yada.handler/error-interceptor-chain])
                           [terminate])]
             (->
              (apply d/chain (cond-> ctx
                               e (assoc :yada/error e)
                               error-data (update :yada/response merge error-data))
                     chain)
              (d/catch Exception
                  (fn [e] ;; TODO
                    e
                    )))))))))

(defrecord ^{:doc ""} Handler []
  clojure.lang.IFn
  (invoke [this req]
    (apply-interceptors
     (ctx/context
      {:yada/handler this
       :ring/request req}))))

(defn handler [model]
  (when-not (s/valid? :yada/handler model)
    (throw
     (ex-info
      (format "Handler model is not valid: %s" (s/explain-str :yada/handler model))
      {:model model :explain (s/explain-data :yada/handler model)})))
  (map->Handler model))


(defn accept-request [^Handler handler req]
  (.invoke handler req))
