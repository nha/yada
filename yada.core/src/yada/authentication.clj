;; Copyright © 2014-2016, JUXT LTD.

(ns yada.authentication
  (:require [yada.context :as ctx]))

(defmulti authenticate-with-scheme "" (fn [scheme ctx] (:yada.resource/scheme scheme)))

(defmethod authenticate-with-scheme :default [scheme ctx]
  nil)

(defn ^:interceptor authenticate [ctx]
  (assoc ctx :yada.request/authentication
         (remove nil?
                 (for [scheme (ctx/authentication-schemes ctx)]
                   (some-> (authenticate-with-scheme scheme ctx)
                           (merge
                            {:yada.request/scheme (:yada.resource/scheme scheme)}
                            (when (:yada.resource/realm scheme)
                              {:yada.request/realm (:yada.resource/realm scheme)})))))))
