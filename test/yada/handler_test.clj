;; Copyright © 2014-2016, JUXT LTD.

(ns yada.handler-test
  (:require [manifold.deferred :as d]
            [yada.context :as ctx :refer [context]]
            [yada.method :refer [http-method perform-method]]
            [yada.handler :refer [handler accept-request]]
            [yada.resource :refer [resource]]
            [yada.test-util :refer [request]]
            [clojure.spec :as s]
            [clojure.test :refer :all]))

(require 'yada.methods)

(deftest various-features
  (let [res (resource {:yada.resource/methods {"PUT" {}}})
        req (request :get "https://localhost")
        h (handler {:yada.handler/interceptor-chain [perform-method yada.handler/terminate]})]
    (is (= 405 (:status @(accept-request h req))))))

;; Work out a safe/debug way to call interceptors by using monads (bind and return)
