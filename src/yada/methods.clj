(ns yada.methods
  (:require
   [clojure.spec :as s]))

(defmulti method "Return a method" (fn [token] token))

(defmethod method "GET" [token] {})
(defmethod method "PUT" [token] {})
(defmethod method "POST" [token] {})
(defmethod method "DELETE" [token] {})
