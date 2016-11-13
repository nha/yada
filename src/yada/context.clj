;; Copyright © 2014-2016, JUXT LTD.

(ns yada.context
  (:require
   [clojure.spec :as s]
   [ring.core.spec :as rs]))

(defrecord Context [])

(s/def :yada/context
  (s/keys :req [:ring/request]))

(defn context [model]
  (when-not (s/valid? :yada/context model)
    (throw
     (ex-info
      (format "Context model is not valid: %s" (s/explain-str :yada/context model))
      {:model model :explain (s/explain-data :yada/context model)})))
  (map->Context model))
