;; Copyright © 2014-2016, JUXT LTD.

(ns yada.jwt-test
  (:require
   [clojure.test :refer :all]
   [yada.resource :refer [resource]]
   [yada.handler :refer [accept-request handler]]
   [yada.method :as method]
   [yada.profile :refer [profiles]]
   [yada.test-util :refer [request]]))

(deftest ok
  (let [res (resource {:yada.resource/methods
                       {"GET" {:yada.resource/response (fn [ctx] "Hello World!")}}})
        h (handler {:yada/resource res
                    :yada.handler/interceptor-chain [method/perform-method yada.handler/terminate]
                    :yada/profile (profiles :dev)})
        response @(accept-request h (request :get "https://localhost"))]
    (is (= 200 (:status response)))
    (is (= "Hello World!" (:body response)))))
