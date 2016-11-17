;; Copyright © 2014-2016, JUXT LTD.

(ns yada.profile-test
  (:require [clojure.test :refer :all]
            [manifold.deferred :as d]
            [yada.context :as ctx :refer [context]]
            [yada.handler :refer [handler accept-request]]
            [yada.resource :refer [resource]]
            [yada.method :refer [perform-method]]
            [yada.test-util :refer [request]]))

(require 'yada.methods)

(deftest profile-test
  )


(let [res (resource {:yada.resource/methods {:get {}}})
      req (request :get "https://localhost")
      h (handler {:yada/resource res
                  :yada.handler/interceptor-chain [perform-method yada.handler/terminate]
                  :yada/profile :dev})
      ]
  @(accept-request h req)

  )
