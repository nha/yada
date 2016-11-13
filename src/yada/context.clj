;; Copyright © 2014-2016, JUXT LTD.

(ns yada.context
  (:require
   [clojure.spec :as s]
   clojure.string
   yada.resource
   [ring.core.spec :as rs]))

(defrecord Context [])

(s/def :yada/context
  (s/keys :req [:ring/request
                :yada/method-token]))

(defn context [model]
  (let [input-spec (s/keys :req [:ring/request])]
    (when-not (s/valid? input-spec model)
      (throw
       (ex-info
        (format "Input context model is not valid: %s"
                (s/explain-str input-spec model))
        {:model model :explain (s/explain-data input-spec model)}))))

  (let [augmented-model
        (-> model
            (assoc :yada/method-token (-> model :ring/request :request-method name clojure.string/upper-case))
            )]
    (when-not (s/valid? :yada/context augmented-model)
      (throw
       (ex-info
        (format "Augmented context model is not value: %s"
                (s/explain-str :yada/context augmented-model))
        {:model augmented-model :explain (s/explain-data :yada/context augmented-model)})))
    (map->Context augmented-model)))
