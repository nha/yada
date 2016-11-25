;; Copyright © 2014-2016, JUXT LTD.

(ns yada.authentication-test
  (:require [clojure.test :refer :all]
            [yada.authentication :as a]
            [yada.resource :refer [new-resource]]))

(defmethod a/authenticate-with-scheme "Test" [scheme ctx]
  {:credentials {:username "alice"}})

(deftest authentication []
  (let [res (new-resource {:yada.resource/authentication-schemes
                       [{:yada.resource/scheme "Test"
                         :yada.resource/realm "default"
                         :yada.resource/authenticate (fn [ctx] ctx)}]
                       :yada.resource/methods {"GET" {:yada.resource/response (fn [ctx] "Hi")}}})
        ctx (a/authenticate {:yada/resource res})]
    (is (= [{:credentials {:username "alice"},
             :yada.request/scheme "Test",
             :yada.request/realm "default"}]
           (:yada.request/authentication ctx)))))
