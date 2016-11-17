;; Copyright © 2014-2016, JUXT LTD.

(ns yada.handler
  (:require [clojure.spec :as s]
            [clojure.spec.gen :as gen]
            [manifold.deferred :as d]
            [yada.method :refer [perform-method]]
            [yada.context :as ctx]
            yada.profile
            yada.response
            [yada.profile :as profile]))

(s/def :yada.handler/interceptor
  (s/with-gen fn? #(gen/return identity)))

(s/def :yada.handler/interceptor-chain
  (s/coll-of :yada.handler/interceptor))

(s/def :yada/handler
  (s/keys :req [:yada/resource
                :yada.handler/interceptor-chain
                :yada/profile]
          :opt [:yada.handler/error-interceptor-chain]))

(defn ^:interceptor terminate [ctx]
  (yada.response/->ring-response (:yada/response ctx)))

(defn apply-interceptors [ctx]

  ;; Have to validate the ctx here, it's the last chance.
  ;; Can't do this in prod
  (let [ctx (assoc ctx
                   :yada/method-token (-> ctx :ring/request :request-method name clojure.string/upper-case)
                   :yada/response (yada.response/new-response))]
    (when (and (profile/validate-context? ctx))
      (when-not (s/valid? :yada/context ctx)
        (throw
         (ex-info
          (format "Context is not valid: %s"
                  (s/explain-str :yada/context ctx))
          {:explain (s/explain-data :yada/context ctx)
           :context ctx}))))

    (let [chain (:yada.handler/interceptor-chain ctx)]
      (->
       (apply d/chain ctx chain)
       (d/catch Exception
           (fn [e]
             (let [error-data (when (instance? clojure.lang.ExceptionInfo e) (ex-data e))
                   chain (or (:yada.handler/error-interceptor-chain ctx)
                             [terminate])]
               (->
                (apply d/chain (cond-> ctx
                                 e (assoc :yada/error e)
                                 (yada.profile/reveal-exception-messages? ctx) (assoc-in [:yada/response :ring.response/body] (.getMessage ^Exception e))
                                 error-data (update :yada/response merge error-data))
                       chain)
                (d/catch Exception
                    (fn [e] ;; TODO
                      e
                      ))))))))))

(defrecord ^{:doc ""} Handler []
  clojure.lang.IFn
  (invoke [this req]
    (apply-interceptors (merge this {:ring/request req}))))

(defn handler [model]
  (map->Handler model))

(defn accept-request [^Handler handler req]
  (.invoke handler req))
