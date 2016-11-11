;; Copyright © 2015, JUXT LTD.

(ns yada.body
  (:require [byte-streams :as bs]
            [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]]
            [manifold.stream :refer [->source transform]]
            [ring.util.http-status :refer [status]]
            [yada.charset :as charset]
            [yada.util :refer [CRLF]])
  (:import java.io.File))

(defprotocol MessageBody
  (to-body [resource representation] "Construct the reponse body for the given resource, given the negotiated representation (metadata)")
  (content-length [_] "Return the size of the resource's representation, if this can possibly be known up-front (return nil if this is unknown)"))

(defmulti render-map (fn [resource representation] (-> representation :media-type :name)))
(defmulti render-seq (fn [resource representation] (-> representation :media-type :name)))

(defn encode-message [s representation]
  (bs/convert s java.nio.ByteBuffer
              {:encoding (or (some-> representation :charset charset/charset)
                             charset/default-platform-charset)}))

(defrecord MapBody [map])

(defn as-body
  "The equivalent of 'quoting' an actual map inside a resource as an
  actual body. Otherwise maps can be treated as a nested value, as
  part of the resource itself. This is the case when using the
  shorthand {:get map} form, rather than {:get {:"
  [map]
  (->MapBody map))


(extend-protocol MessageBody

  (class (byte-array 0))
  (to-body [ba representation]
    ;; TODO: For text representations, try to encode into charset first
    ba)
  (content-length [ba] (count ba))

  String
  ;; A String is already its own representation, all we must do now is encode it to a char buffer
  (to-body [s representation]
    (encode-message s representation))

  Long
  (to-body [l representation]
    (to-body (str l) representation))

  ;; The content-length is NOT the length of the string, but the
  ;; "decimal number of octets, for a potential payload body".
  ;; See  http://mark.koli.ch/remember-kids-an-http-content-length-is-the-number-of-bytes-not-the-number-of-characters
  (content-length [s]
    nil)

  MapBody
  (to-body [mb representation]
    (to-body (:map mb) representation))

  clojure.lang.APersistentMap
  (to-body [m representation]
    ;; We always try to arrive at a string which is then converted
    (encode-message (render-map m representation) representation))

  clojure.lang.APersistentVector
  (to-body [v representation]
    (encode-message (render-seq v representation) representation))

  clojure.lang.ASeq
  (to-body [v representation]
    (encode-message (render-seq v representation) representation))

  clojure.lang.LazySeq
  (to-body [v representation]
    (encode-message (render-seq v representation) representation))

  java.util.HashSet
  (to-body [v representation]
    (encode-message (render-seq v representation) representation))

  File
  (to-body [f _]
    ;; The file can simply go into the body of the Ring response. We
    ;; could transcode it according to the charset if only we knew what
    ;; the file's initial encoding was. (We can't know this.)
    f)
  (content-length [f] (.length f))

  java.nio.ByteBuffer
  (to-body [b _] b)
  (content-length [b] (.remaining b))

  ;; The default pass-through to the web server
  Object
  (to-body [s _] s)
  (content-length [_] nil)

  nil
  (to-body [_ _] nil)
  (content-length [_] 0))

;; application/edn

(defmethod render-map "application/edn"
  [m representation]
  (let [pretty (get-in representation [:media-type :parameters "pretty"])]
    (if pretty
      (with-out-str (pprint m))
      (prn-str m))))

(defmethod render-seq "application/edn"
  [s representation]
  (let [pretty (get-in representation [:media-type :parameters "pretty"])]
    (if pretty
      (with-out-str (pprint s))
      (prn-str s))))

;; defaults

(defmethod render-map :default
  [m representation]
  (throw (ex-info (format "No implementation for render-map for media-type: %s" (:name (:media-type representation)))
                  {:representation representation})))

(defmethod render-seq :default
  [m representation]
  (throw (ex-info (format "No implementation for render-seq for media-type: %s" (:name (:media-type representation)))
                  {:representation representation})))

;; Errors

(def ^{:dynamic true
       :doc "When set to logical true, errors will be printed. Defaults to true."}
  *output-errors* true)

(def ^{:dynamic true
       :doc "When set to logical true, errors will be output with their stack-traces. Defaults to true."}
  *output-stack-traces* true)

(defmulti render-error (fn [status error representation ctx]
                         (-> representation :media-type :name)))

(defn get-error-message [code]
  (or (some-> status (get code) :name) "Unknown"))

(defn get-error-description [code]
  (some-> status (get code) :description))

(defmethod render-error "application/edn"
  [status error representation {:keys [id options]}]
  {:status status
   :message (get-error-message status)
   :id id
   :error error})

(defmethod render-error "text/plain"
  [status error representation {:keys [id options]}]
  (if (instance? clojure.lang.ExceptionInfo error)
    ;; TODO: pprint uses Java system property line.separator, consider
    ;; using fip or one that can print CRLF line endings.
    (str (.getMessage error) CRLF CRLF (with-out-str (pprint (ex-data error))))
    (str (.getMessage error) CRLF)))

(defmethod render-error :default
  [status error representation {:keys [id options]}]
  nil)
