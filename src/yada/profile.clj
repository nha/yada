;; Copyright © 2014-2016, JUXT LTD.

(ns ^{:doc "A profile defines what yada does in certain
    cases. Profiles typically reflect operating environments, mapping
    onto runtime profiles such as :dev and :prod"} yada.profile
  (:require
   [clojure.spec :as s]
   [manifold.deferred :as d]
   [yada.context :as ctx]))

(s/def :yada.profile/nil-response-fn fn?)
(s/def :yada.profile/reveal-exception-messages? (s/or :boolean boolean? :fn fn?))

(s/def :yada/profile (s/keys :req [:yada.profile/nil-response-fn
                                   :yada.profile/reveal-exception-messages?]))

(defn nil-response-fn [ctx]
  (when-let [f (get-in ctx [:yada/profile :yada.profile/nil-response-fn])]
    (f ctx)))

(defn reveal-exception-messages? [ctx]
  (let [f (get-in ctx [:yada/profile :yada.profile/nil-response-fn])]
    (cond
      (boolean? f) f
      (fn? f) (f ctx))))

(def profiles
  {:dev
   {:yada.profile/nil-response-fn
    (fn [ctx]
      (d/error-deferred
       (ex-info (format "No response function declared in resource for method %s" (ctx/method-token ctx))
                {:ring.response/status 500})))
    :yada.profile/reveal-exception-messages? true}

   :prod
   {:yada.profile/nil-response-fn
    (fn [ctx]
      (d/error-deferred
       (ex-info "" {:ring.response/status 500})))
    :yada.profile/reveal-exception-messages? false}})
