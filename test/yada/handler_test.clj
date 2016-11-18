;; Copyright © 2014-2016, JUXT LTD.

(ns yada.handler-test
  (:require [clojure.test :refer :all]
            [yada.handler :refer [accept-request handler]]
            [yada.method :refer [perform-method]]
            [yada.profile :refer [profiles]]
            [yada.resource :refer [resource]]
            [yada.test-util :refer [request]]))

(require 'yada.methods)

(deftest happy-path
  (let [res (resource {:yada.resource/methods
                       {"GET" {:yada.resource/response (fn [ctx] "Hello World!")}}})
        req (request :get "https://localhost")
        h (handler {:yada/resource res
                    :yada.handler/interceptor-chain [perform-method yada.handler/terminate]
                    :yada/profile (profiles :dev)})
        response @(accept-request h req)]

    (is (= 200 (:status response)))
    (is (= "Hello World!" (:body response)))))

(deftest no-such-method
  (let [res (resource {:yada.resource/methods {"PUT" {}}})
        req (request :get "https://localhost")
        h (handler {:yada/resource res
                    :yada.handler/interceptor-chain [perform-method yada.handler/terminate]
                    :yada/profile (profiles :dev)})
        response @(accept-request h req)
        ]
    (is (= 405 (:status response)))
    (is (= "No matching method in resource" (:body response)))))
