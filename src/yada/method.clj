(ns yada.method
  (:require
   yada.context
   yada.resource
   yada.spec
   [clojure.spec :as s]
   [manifold.deferred :as d]))

(s/def :yada.method/safe? boolean?)

;; (s/def :yada.method/proxy (s/fspec :args (s/cat :context :yada/context)))

(s/def :yada.method/method (s/keys :req [:yada.method/safe?
                                         ;; :yada.method/proxy
                                         ]))

(defmulti http-method "Return a map representing the HTTP method" (fn [token] token))

(defn ^:interceptor perform-method [ctx]
  (let [method (http-method (yada.context/method-token ctx))]
    (when-let [pf (:yada.method/wrapper method)]
      (pf ctx))))
