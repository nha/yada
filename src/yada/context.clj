;; Copyright © 2014-2016, JUXT LTD.

(ns yada.context
  (:require
   [clojure.spec :as s]
   clojure.string
   yada.resource
   yada.response
   [ring.core.spec :as rs]))

(s/def :yada/context
  (s/keys :req [:yada/resource
                :ring/request

                :yada/method-token
                :yada/response
                :yada/profile
                ]))

(defn method-token [ctx]
  (-> ctx :yada/method-token))

(defn lookup-method [ctx]
  (yada.resource/lookup-method
   (:yada/resource ctx)
   (:yada/method-token ctx)))
