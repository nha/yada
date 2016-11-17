;; Copyright © 2014-2016, JUXT LTD.

(ns yada.handler-test
  (:require [clojure.test :refer :all]
            [yada.handler :refer [accept-request handler]]
            [yada.method :refer [perform-method]]
            [yada.profile :refer [profiles]]
            [yada.resource :refer [resource]]
            [yada.test-util :refer [request]]))

(require 'yada.methods)

(deftest various-features
  (let [res (resource {:yada.resource/methods {"PUT" {}}})
        req (request :get "https://localhost")
        h (handler {:yada/resource res
                    :yada.handler/interceptor-chain [perform-method yada.handler/terminate]
                    :yada/profile (profiles :dev)})]
    (is (= 405 (:status @(accept-request h req))))))


;; Work out a safe/debug way to call interceptors by using monads (bind and return)
