;; Copyright © 2015, JUXT LTD.

(ns yada.request-body
  (:require
   [byte-streams :as b]
   [clojure.tools.logging :refer :all]
   [ring.swagger.schema :as rs]
   [manifold.deferred :as d]
   [manifold.stream :as s]))

(defmulti process-request-body
  (fn [ctx body-stream content-type & args]
    (:name content-type)))

(defmethod process-request-body :default
  [ctx body-stream media-type & args]
  (d/error-deferred (ex-info "Unsupported Media Type" {:status 418})))

(defmethod process-request-body "application/octet-stream"
  [ctx body-stream media-type & args]
  (d/chain
   (s/reduce (fn [acc buf] (inc acc)) 0 body-stream)
   (fn [acc]
     (infof ":default acc is %s" acc))
   )
  ctx)

;; Deprecated?

;; Coerce request body  ------------------------------

;; The reason we use 2 forms for coerce-request-body is so that
;; schema-using forms can call into non-schema-using forms to
;; pre-process the body.

#_(defmulti coerce-request-body (fn [body media-type & args] media-type))

#_(defmethod coerce-request-body "application/json"
  ([body media-type schema]
   (rs/coerce schema (coerce-request-body body media-type) :json))
  ([body media-type]
   (json/decode body keyword)))

#_(defmethod coerce-request-body "application/octet-stream"
  [body media-type schema]
  (cond
    (instance? String schema) (bs/to-string body)
    :otherwise (bs/to-string body)))

#_(defmethod coerce-request-body nil
  [body media-type schema] nil)

#_(defmethod coerce-request-body "application/x-www-form-urlencoded"
  ([body media-type schema]
   (rs/coerce schema (coerce-request-body body media-type) :query))
  ([body media-type]
   (keywordize-keys (codec/form-decode body))))
