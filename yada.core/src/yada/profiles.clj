(ns yada.profiles
  (:require
   [yada.spec :refer [validate]]
   [yada.context :as ctx]
   [manifold.deferred :as d]))

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
    :yada.profile/validate-set-cookie? true
    :yada.profile/interceptor-wrapper debug-interceptor-wrapper}

   :prod
   {:yada.profile/nil-response-fn
    (fn [ctx]
      (d/error-deferred
       (ex-info "" {:ring.response/status 500})))
    :yada.profile/reveal-exception-messages? false
    :yada.profile/validate-context? false
    :yada.profile/validate-set-cookie? false}})
