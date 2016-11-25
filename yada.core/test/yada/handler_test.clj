;; Copyright © 2014-2016, JUXT LTD.

(ns yada.handler-test
  (:require [clojure.test :refer :all]
            [yada.handler :refer [new-handler accept-request]]
            [yada.method :as method]
            [yada.profiles :refer [profiles]]
            [yada.resource :refer [new-resource]]
            [yada.test-util :refer [new-request]]))

(require 'yada.methods)

(deftest ok
  (let [res (new-resource {:yada.resource/methods
                           {"GET" {:yada.resource/response (fn [ctx] "Hello World!")}}})
        h (new-handler {:yada/resource res
                        :yada.handler/interceptor-chain [method/perform-method]
                        :yada/profile (profiles :dev)})
        response @(accept-request h (new-request :get "https://localhost"))]
    (is (= 200 (:status response)))
    (is (= "Hello World!" (:body response)))))

(deftest no-such-method
  (let [res (new-resource {:yada.resource/methods {"PUT" {}}})
        h (new-handler {:yada/resource res
                        :yada.handler/interceptor-chain [method/perform-method]
                        :yada/profile (profiles :dev)})
        response @(accept-request h (new-request :get "https://localhost"))]
    (is (= 405 (:status response)))
    (is (= "Method Not Allowed" (:body response)))))

(deftest not-implemented
  (let [res (new-resource {:yada.resource/methods {"BREW" {}}})
        h (new-handler {:yada/resource res
                        :yada.handler/interceptor-chain [method/check-method-implemented]
                        :yada/profile (profiles :dev)})
        response @(accept-request h (new-request :brew "https://localhost"))]
    (is (= 501 (:status response)))
    (is (= "Not Implemented" (:body response)))))
