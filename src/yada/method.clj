(ns yada.method
  (:require
   yada.context
   yada.resource
   [clojure.spec :as s]
   [manifold.deferred :as d]))

(s/def :yada.method/safe? boolean?)

;; (s/def :yada.method/proxy (s/fspec :args (s/cat :context :yada/context)))

(s/def :yada.method/method (s/keys :req [:yada.method/safe?
                                         ;; :yada.method/proxy
                                         ]))

(defmulti http-method "Return a map representing the HTTP method" identity)
