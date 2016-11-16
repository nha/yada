;; Copyright © 2014-2016, JUXT LTD.

(ns yada.response
  (:require
   [clojure.spec :as s]
   ring.core.spec))

(defrecord
    ^{:doc "This record is used as an escape mechanism users to
    return it in method responses. Doing this indicates to yada that
    the user knows what they are doing and want fine grained control
    over the response, perhaps setting cookies, response headers and
    so on."}
    Response [])

(s/def :yada/response
  (s/keys :req [:ring.response/headers]
          :opt [:ring.response/status]))

(defn new-response []
  (map->Response {:ring.response/headers {}}))

(defn ->ring-response [response]
  (merge
   {:status (or (:ring.response/status response) 500)
    :headers (or (:ring.response/headers response) {})}
   (when-let [body (:ring.response/body response)]
     {:body body})))
