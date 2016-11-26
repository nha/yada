;; Copyright © 2014-2016, JUXT LTD.

(defproject yada-parent "2.0.0-alpha1-SNAPSHOT"
  :description "The yada core"
  :url "http://github.com/juxt/yada"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}

  :plugins [[lein-modules "0.3.11"]]



  :profiles {:provided
             {:dependencies [[org.clojure/clojure "_"]]}}

  :modules {:inherited
            {:url "http://github.com/juxt/yada"
             :license {:name "The MIT License"
                       :url "http://opensource.org/licenses/MIT"}}

            :versions {org.clojure/clojure "1.9.0-alpha14"
                       yada.core "2.0.0-alpha1"
                       yada.jwt "2.0.0-alpha1"
                       yada "2.0.0-alpha1"}

            :dirs ["yada.core"
                   "yada.jwt"
                   "yada.yada"]}

;;  :exclusions [org.clojure/clojure]

  )
