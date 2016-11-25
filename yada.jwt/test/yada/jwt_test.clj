;; Copyright © 2014-2016, JUXT LTD.

(ns yada.jwt-test
  (:require [clojure.test :refer :all]
            [yada.authentication :as a]
            [yada.resource :refer [new-resource]]
            [buddy.sign.jwt :as jwt]
            [yada.test-util :refer [new-request]]))

(jwt/sign {:foo 1} "secret")

(deftest authentication []
  (let [res (new-resource {:yada.resource/authentication-schemes
                           [{:yada.resource/scheme :jwt
                             :yada.resource/realm "default"
                             :yada.resource/authenticate (fn [ctx] ctx)}]
                           :yada.resource/methods {"GET" {:yada.resource/response (fn [ctx] "Hi")}}})
        ctx (a/authenticate {:yada/resource res})]
    (is (= [{:credentials {:username "alice"},
             :yada.request/scheme "Test",
             :yada.request/realm "default"}]
           (:yada.request/authentication ctx)))))
