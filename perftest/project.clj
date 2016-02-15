;; Copyright © 2015, JUXT LTD.

(defproject yada.perftest "1.1.0-SNAPSHOT"
  :description "Performance testing yada"
  :url "http://github.com/juxt/yada"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}

  :dependencies
  [[org.clojure/clojure "1.7.0"]
   [clj-gatling "0.7.9"]
   [clj-http "2.0.1"]
   [byte-streams "0.2.1-alpha2" :exclusions [clj-tuple]]]

  :pedantic? :abort)

