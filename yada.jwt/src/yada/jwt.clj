;; Copyright © 2014-2016, JUXT LTD.

(ns yada.jwt
  (:require [yada.authentication :as a]
            [buddy.sign.jws :as jws]))

(jws/sign)

(defmethod a/authenticate-with-scheme :jwt [scheme ctx]
  {:credentials {:username "alice"
                 :scheme scheme}})
