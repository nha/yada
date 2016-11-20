;; Copyright © 2014-2016, JUXT LTD.

(defproject yada "2.0.0-alpha1-SNAPSHOT"
  :description "The yada core"
  :url "http://github.com/juxt/yada"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}

  :plugins [[lein-modules "0.3.11"]]

  :modules {:dirs ["yada.core"
                   "yada.jwt"
                   "yada.yada"]
            :versions {org.clojure/clojure "1.9.0-alpha14"}}

;;  :exclusions [org.clojure/clojure]

  )
