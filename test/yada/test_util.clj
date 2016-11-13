;; Copyright © 2014-2016, JUXT LTD.

(ns yada.test-util
  (:require  [clojure.test :as t]))

(defn request [method uri]
  (let  [uri (java.net.URI. uri)
         scheme (keyword (or (.getScheme uri) "http"))
         host (or (.getHost uri) "localhost")
         port (when (not= (.getPort uri) -1) (.getPort uri))
         path (.getRawPath uri)
         query(.getRawQuery uri)]
    {:server-port (or port ({:http 80 :https 443} scheme))
     :server-name host
     :remote-addr "localhost"
     :uri (if (clojure.string/blank? path) "/" path)
     :scheme scheme
     :protocol "HTTP/1.1"
     :request-method method
     :headers {"host" (if port
                        (str host ":" port)
                        host)}}))
