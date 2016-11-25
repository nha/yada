;; Copyright © 2014-2016, JUXT LTD.

(ns yada.cookies-test
  (:require [clojure.test :refer :all]
            [clojure.spec :as s]
            [clojure.spec.gen :as gen]
            [yada.context :as ctx]
            [yada.handler :refer [new-handler accept-request]]
            [yada.profiles :refer [profiles]]
            [yada.method :refer [perform-method]]
            [yada.resource :refer [new-resource]]
            [yada.cookies :as cookies]
            [yada.test-util :refer [new-request]]))

(deftest ->headers-test
  ;; TODO: property based test
  (is (map? (cookies/->headers (gen/generate (s/gen :yada.response/cookies))))))

(deftest set-cookie-test
  (let [res (new-resource
             {:yada.resource/methods
              {"GET" {:yada.resource/response
                      (fn [ctx]
                        (-> (ctx/response ctx)
                            (cookies/set-cookie ctx "foo" {:yada.cookie/value "bar"})
                            (ctx/set-body "Hello World!")))}}})

        h (new-handler {:yada/resource res
                        :yada.handler/interceptor-chain [perform-method]
                        :yada/profile (profiles :dev)})

        req (-> (new-request :get "https://localhost")
                (assoc-in [:headers "cookie"] "SID=31d4d96e407aad42"))

        response @(accept-request h (new-request :get "https://localhost"))]
    (is (= 200 (:status response)))
    (is (= ["foo=bar"] (get-in response [:headers "Set-Cookie"])))))

(deftest get-cookie-test
  (let [res (new-resource
             {:yada.resource/methods
              {"GET" {:yada.resource/response (fn [ctx] "Hello World!")}}})

        h (new-handler {:yada/resource res
                        :yada.handler/interceptor-chain []
                        :yada/profile (profiles :dev)})

        req (-> (new-request :get "https://localhost")
                (assoc-in [:headers "cookie"] "SID=31d4d96e407aad42"))
        ctx (ctx/context (into {} h) req)]
    (is (= {"SID" {:value "31d4d96e407aad42"}}
           (cookies/cookies ctx)))))
