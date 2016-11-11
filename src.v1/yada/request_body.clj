;; Copyright © 2015, JUXT LTD.

(ns yada.request-body
  (:require [byte-streams :as bs]
            [clojure.edn :as edn]
            [clojure.tools.logging :refer :all]
            [manifold.deferred :as d]
            [manifold.stream :as s]
            [ring.util.codec :as codec]
            [ring.util.request :as req]
            [yada.media-type :as mt]))

(def application-octet-stream
  (mt/string->media-type "application/octet-stream"))

(defmulti process-request-body
  "Process the request body, filling out and coercing any parameters
  identified in the context."
  (fn [ctx body-stream content-type & args]
    content-type))

;; See rfc7231#section-3.1.1.5 - we should assume application/octet-stream

;; We return 415 if there's a content-type which we don't
;; recognise. Using the multimethods :default method is a way of
;; returning a 415 even if the resource declares that it consumes an
;; (unsupported) media type.
(defmethod process-request-body :default
  [ctx body-stream media-type & args]
  (d/error-deferred (ex-info "Unsupported Media Type" {:status 415})))

;; A nil (or missing) Content-Type header is treated as
;; application/octet-stream.
(defmethod process-request-body nil
  [ctx body-stream media-type & args]
  (process-request-body ctx body-stream application-octet-stream))

(defmethod process-request-body "application/octet-stream"
  [ctx body-stream media-type & args]
  ctx)
