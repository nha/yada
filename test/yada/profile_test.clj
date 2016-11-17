;; Copyright © 2014-2016, JUXT LTD.

(ns yada.profile-test
  (:require [clojure.test :refer :all]
            [manifold.deferred :as d]
            [yada.context :as ctx :refer [context]]
            [yada.handler :refer [handler accept-request]]
            [yada.resource :refer [resource]]
            [yada.method :refer [perform-method]]
            [yada.profile :refer [profiles]]
            [yada.test-util :refer [request]]
            [clojure.spec :as s]))

(require 'yada.methods)

(deftest profile-test
  (let [res (resource {:yada.resource/methods {:get {}}})
        req (request :get "https://localhost")
        h {:yada/resource res
           :yada.handler/interceptor-chain [perform-method yada.handler/terminate]}]

    (testing "dev profile"
      (let [h (handler (assoc h :yada/profile (:dev profiles)))
            response @(accept-request h req)]
        (is (= 500 (:status response)))
        (is (= "No response function declared in resource for method GET" (:body response)))))

    (testing "prod profile"
      (let [h (handler (assoc h :yada/profile (:prod profiles)))
            response @(accept-request h req)]
        (is (= 500 (:status response)))
        (is (= "" (:body response)))))))
