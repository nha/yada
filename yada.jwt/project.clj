;; Copyright © 2014-2016, JUXT LTD.

(defproject yada.jwt "2.0.0-alpha1"
  :plugins [[lein-modules "0.3.11"]]

  :dependencies
  [[yada.core "_"]
   [buddy/buddy-sign "1.3.0"]]

;;  :pedantic? :abort

  :global-vars {*warn-on-reflection* true}

  :repl-options {:init-ns user
                 :welcome (println "Type (dev) to start")}

  :profiles
  {:dev {:jvm-opts ["-Xms1g" "-Xmx1g"
                    "-server"
                    "-Dio.netty.leakDetectionLevel=paranoid"]

;;         :pedantic? :abort

         :dependencies
         [[org.clojure/clojure "1.9.0-alpha14"]

          [ch.qos.logback/logback-classic "1.1.5"
           :exclusions [org.slf4j/slf4j-api]]
          [org.slf4j/jul-to-slf4j "1.7.18"]
          [org.slf4j/jcl-over-slf4j "1.7.18"]
          [org.slf4j/log4j-over-slf4j "1.7.18"]

          [aleph "0.4.2-alpha8"]]}})
