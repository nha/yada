;; Copyright © 2014-2016, JUXT LTD.

(ns ^{:doc "A profile defines what yada does in certain
    cases. Profiles typically reflect operating environments, mapping
    onto runtime profiles such as :dev and :prod"} yada.profile
  (:require
   [clojure.spec :as s]
   [manifold.deferred :as d]
   [yada.context :as ctx]
   [yada.spec :refer [validate]]))

(s/def :yada.profile/nil-response-fn fn?)
(s/def :yada.profile/reveal-exception-messages? (s/or :boolean boolean? :fn fn?))
(s/def :yada.profile/validate-context? boolean?)
(s/def :yada.profile/interceptor-wrapper fn?)

(s/def :yada/profile (s/keys :req [:yada.profile/nil-response-fn
                                   :yada.profile/reveal-exception-messages?]
                             :opt [:yada.profile/interceptor-wrapper]))

(defn nil-response-fn [ctx]
  (when-let [f (get-in ctx [:yada/profile :yada.profile/nil-response-fn])]
    (f ctx)))

(defn reveal-exception-messages? [ctx]
  (let [f (get-in ctx [:yada/profile :yada.profile/nil-response-fn])]
    (cond
      (boolean? f) f
      (fn? f) (f ctx))))

(defn validate-context? [ctx]
  (get-in ctx [:yada/profile :yada.profile/validate-context?]))

(defn interceptor-wrapper [ctx]
  (get-in ctx [:yada/profile :yada.profile/interceptor-wrapper]))

(defn debug-interceptor-wrapper [i]
  (fn [ctx]
    (validate ctx :yada/context (format "Context not valid on entering interceptor: %s" i))
    (i ctx)))

(def profiles
  {:dev
   {:yada.profile/nil-response-fn
    (fn [ctx]
      (d/error-deferred
       (ex-info (format "No response function declared in resource for method %s" (ctx/method-token ctx))
                {:ring.response/status 500})))
    :yada.profile/reveal-exception-messages? true
    :yada.profile/validate-context? true
    :yada.profile/interceptor-wrapper debug-interceptor-wrapper}

   :prod
   {:yada.profile/nil-response-fn
    (fn [ctx]
      (d/error-deferred
       (ex-info "" {:ring.response/status 500})))
    :yada.profile/reveal-exception-messages? false
    :yada.profile/validate-context? false}})
