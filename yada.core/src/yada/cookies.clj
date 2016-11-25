;; Copyright © 2014-2016, JUXT LTD.

(ns yada.cookies
  (:require
   [clojure.string :as str]
   [clojure.spec :as s]
   clojure.string
   yada.resource
   [yada.profile :as p]
   [ring.core.spec :as rs]
   [yada.spec :refer [validate]]
   yada.response
   [ring.middleware.cookies :refer [cookies-request cookies-response]])
  (:import [yada.response Response]))

(s/def :yada.request/cookies (s/map-of string? string?))

;; Response cookies

(s/def :yada.cookie/value string?)

(s/def :yada.cookie/domain string?)
(s/def :yada.cookie/path string?)
(s/def :yada.cookie/secure boolean?)
(s/def :yada.cookie/http-only boolean?)
(s/def :yada.cookie/max-age integer?)
(s/def :yada.cookie/expires inst?)

(extend-protocol Inst
  java.time.Instant
  (inst-ms* [inst] (.toEpochMilli ^java.time.Instant inst)))

(s/def :yada.response/cookie
  (s/keys :req [:yada.cookie/value]
          :opt [:yada.cookie/domain
                :yada.cookie/path
                :yada.cookie/secure
                :yada.cookie/http-only
                :yada.cookie/max-age
                :yada.cookie/expires]))

(s/def :yada.response/cookies (s/map-of string? :yada.response/cookie))

(defn cookies [ctx]
  (deref (get-in ctx [:yada/request :yada.request/cookies*])))

(defn set-cookie [response ctx nm val]
  (assert (instance? Response response) "Response parameter is not a response record")
  (when (p/validate-set-cookie? ctx)
    (validate val :yada.response/cookie "Invalid cookie value"))
  (assoc-in response [:yada.response/cookies nm] val))

(defn ->headers [cookies]
  (:headers
   (cookies-response
    {:cookies (into {} (for [[k {:yada.cookie/keys [value]}] cookies]
                         [k {:value (or value (with-out-str (prn cookies)))}]
                         ))})))
