;; Copyright © 2014-2016, JUXT LTD.

(ns yada.handler-test
  (:require
   clojure.string
   [clojure.spec :as s]
   [clojure.test :refer :all]
   [manifold.deferred :as d]
   [yada.resource :refer [resource]]
   [yada.profile :as profile]
   [yada.context :refer [new-context] :as ctx]
   [clojure.test.check.generators :as gen]
   [yada.method :refer [http-method]]
   ring.core.spec
   [yada.test-util :refer [request]]
   yada.methods
   ))

(defn perform-operation [ctx]
  ;; Invoke the method - how does yada-v1 do this?

  (let [method (http-method (ctx/method-token ctx))]
    #_(when-not (s/valid? :yada.method/method m)
      (throw
       (ex-info (format "Method validation failed: %s" (s/explain-str :yada.method/method method))
                {:method method :explain (s/explain-data :yada.method/method method)})))
    (if-let [pf (:yada.method/wrapper method)]
      (pf ctx)
      (throw (ex-info "No wrapper for method" {:method method})))))

(let [res (resource {:yada.resource/methods {"PUT" {}}})
      req (request :get "https://localhost")
      ctx (new-context {:ring/request req})]
  @(apply d/chain ctx [perform-operation]))



;; Work out a safe/debug way to call interceptors by using monads (bind and return)
