;; Copyright © 2014-2016, JUXT LTD.

(ns yada.profile-test
  (:require [clojure.test :refer :all]
            [yada.handler :refer [new-handler accept-request]]
            [yada.method :refer [perform-method]]
            [yada.profiles :refer [profiles]]
            [yada.resource :refer [new-resource]]
            [yada.test-util :refer [new-request]]))

(require 'yada.methods)

(deftest profile-test
  (let [res (new-resource {:yada.resource/methods {:get {}}})
        req (new-request :get "https://localhost")
        h {:yada/resource res
           :yada.handler/interceptor-chain [perform-method]}]

    (testing "dev profile"
      (let [h (new-handler (assoc h :yada/profile (:dev profiles)))
            response @(accept-request h req)]
        (is (= 500 (:status response)))
        (is (= "Internal Server Error" (:body response)))))

    (testing "prod profile"
      (let [h (new-handler (assoc h :yada/profile (:prod profiles)))
            response @(accept-request h req)]
        (is (= 500 (:status response)))
        (is (= "Internal Server Error" (:body response)))))))
