(ns yada.method
  (:require
   yada.context
   yada.resource
   yada.spec
   [clojure.spec :as s]
   [manifold.deferred :as d]))

(s/def :yada.method/safe? boolean?)

(s/def :yada.method/method (s/keys :req [:yada.method/safe?
                                         :yada.method/wrapper]))

(defmulti http-method "Return a map representing the HTTP method" (fn [token] token))

(defn ^:interceptor check-method-implemented [ctx]
  (let [token (yada.context/method-token ctx)]
    (if-not (contains? (set (keys (methods http-method))) token)
      (d/error-deferred (ex-info (format "No defmethod defined for %s" token)
                                 {:ring.response/status 501
                                  :yada/method-token token}))
      ctx)))

(defn ^:interceptor perform-method [ctx]
  (let [method (http-method (yada.context/method-token ctx))]
    (when-let [pf (:yada.method/wrapper method)]
      (pf ctx))))
