;; Copyright © 2014-2016, JUXT LTD.

(ns yada.handler-test
  (:require
   clojure.string
   [clojure.spec :as s]
   [clojure.test :refer :all]
   [manifold.deferred :as d]
   [yada.resource :refer [resource]]
   [yada.context :refer [context]]
   [clojure.test.check.generators :as gen]
   [yada.methods :refer [method]]
   ring.core.spec
   [yada.test-util :refer [request]]
   ))

(defn perform-operation [ctx]
  ;; Invoke the method - how does yada-v1 do this?
  ;;(assoc ctx :method-token )
  ctx
  )

(let [res (resource {:yada/methods {"PUT" {}}})
      req (request :get "https://localhost")
      ctx (context {:ring/request req})]
  @(apply d/chain ctx [perform-operation]))


;; Work out a safe/debug way to call interceptors by using monads (bind and return)
