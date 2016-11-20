;; Copyright © 2014-2016, JUXT LTD.

(ns yada.handler-test
  (:require [clojure.test :refer :all]
            [yada.handler :refer [accept-request handler]]
            [yada.method :as method]
            [yada.profile :refer [profiles]]
            [yada.resource :refer [resource]]
            [yada.test-util :refer [request]]))

(require 'yada.methods)

(deftest ok
  (let [res (resource {:yada.resource/methods
                       {"GET" {:yada.resource/response (fn [ctx] "Hello World!")}}})
        h (handler {:yada/resource res
                    :yada.handler/interceptor-chain [method/perform-method]
                    :yada/profile (profiles :dev)})
        response @(accept-request h (request :get "https://localhost"))]
    (is (= 200 (:status response)))
    (is (= "Hello World!" (:body response)))))

(deftest no-such-method
  (let [res (resource {:yada.resource/methods {"PUT" {}}})
        h (handler {:yada/resource res
                    :yada.handler/interceptor-chain [method/perform-method]
                    :yada/profile (profiles :dev)})
        response @(accept-request h (request :get "https://localhost"))]
    (is (= 405 (:status response)))
    (is (= "Method Not Allowed" (:body response)))))

(deftest not-implemented
  (let [res (resource {:yada.resource/methods {"BREW" {}}})
        h (handler {:yada/resource res
                    :yada.handler/interceptor-chain [method/check-method-implemented]
                    :yada/profile (profiles :dev)})
        response @(accept-request h (request :brew "https://localhost"))]
    (is (= 501 (:status response)))
    (is (= "Not Implemented" (:body response)))))
