;; Copyright © 2014-2016, JUXT LTD.

(ns ^{:doc "A profile defines what yada does in certain
    cases. Profiles typically reflect operating environments, mapping
    onto runtime profiles such as :dev and :prod"} yada.profile
  (:require
   [clojure.spec :as s]
   [manifold.deferred :as d]
   [yada.context :as ctx]))

(s/def :yada/profile keyword?)

(defmulti
  ^{:doc "Has the user failed to specify the optional :yada/response fn? Methods should return a modified request object"}
  nil-response-fn
  (fn [ctx] (:yada/profile ctx)))

(defmethod nil-response-fn :dev [ctx]
  (d/error-deferred
   (ex-info (format "No response function declared in resource for method %s" (ctx/method-token ctx))
            {:ring.response/status 500})))


(defmulti reveal-exception-messages? (fn [ctx] (:yada/profile ctx)))

(defmethod reveal-exception-messages? :dev [ctx] true)

(defmethod reveal-exception-messages? :default [ctx] false)

;; TODO: Profiles feel like they should be spec'd maps too
