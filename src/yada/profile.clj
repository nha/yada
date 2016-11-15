;; Copyright © 2014-2016, JUXT LTD.

(ns ^{:doc "A profile defines what yada does in certain
    cases. Profiles typically reflect operating environments, mapping
    onto runtime profiles such as :dev and :prod"} yada.profile
  (:require [clojure.spec :as s]))

(s/def :yada/profile keyword?)

(defmulti
  ^{:doc "Has the user failed to specify the optional :yada/response fn? Methods should return a modified request object"}
  nil-response-fn
  (fn [ctx] (:yada/profile ctx)))

(defmethod nil-response-fn :dev [ctx]
  ctx)
