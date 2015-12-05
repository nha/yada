;; Copyright © 2015, JUXT LTD.

(ns yada.coercion-test
  (:require
   [clojure.test :refer :all]
   [yada.media-type :as mt]
   [schema.core :as s]
   [schema.coerce :as sc]
   [schema.utils :refer [error?]])
  (:import [yada.charset CharsetMap]
           [yada.media_type MediaTypeMap]
           [java.util Date]))

(s/defschema RepresentationSet
  {:media-type #{MediaTypeMap}
   ;;(s/optional-key :charset) #{CharsetMap}
   ;;(s/optional-key :encoding) #{String}
   ;;(s/optional-key :language) #{String}
   })

(def resource-schema
  {(s/optional-key :produces) [RepresentationSet]})

(defprotocol MediaTypeCoercion
  (as-media-type [_] ""))

(defprotocol SetCoercion
  (as-set [_] ""))

(defprotocol VectorCoercion
  (as-vector [_] ""))

(defprotocol RepresentationSetCoercion
  (as-representation-set [_] ""))

(extend-protocol SetCoercion
  clojure.lang.PersistentHashSet
  (as-set [s] s)
  Object
  (as-set [s] #{s}))

(extend-protocol VectorCoercion
  clojure.lang.PersistentVector
  (as-vector [v] v)
  Object
  (as-vector [o] [o]))

(extend-protocol MediaTypeCoercion
  MediaTypeMap
  (as-media-type [mt] mt)
  String
  (as-media-type [s] (mt/string->media-type s)))

(extend-protocol RepresentationSetCoercion
  clojure.lang.PersistentArrayMap
  (as-representation-set [m] m)
  String
  (as-representation-set [s] {:media-type s}))

(defn get-representationset-coercer []
  (sc/coercer RepresentationSet {#{MediaTypeMap} as-set
                                 MediaTypeMap as-media-type}))

(defn get-coercer []
  (let [mappings {[RepresentationSet] as-vector
                  RepresentationSet as-representation-set
                  #{MediaTypeMap} as-set
                  MediaTypeMap as-media-type}]
    (sc/coercer resource-schema mappings)))

(def TEXT_HTML (mt/string->media-type "text/html"))

(deftest produces-test
  (let [coercer (get-coercer)]
    (testing "as-vector"
      (is (= (coercer {:produces {:media-type #{TEXT_HTML}}})
             {:produces [{:media-type #{TEXT_HTML}}]})))

    (testing "to-set"
      (is (= (coercer {:produces {:media-type TEXT_HTML}})
             {:produces [{:media-type #{TEXT_HTML}}]})))

    (testing "string"
        (is (= (coercer {:produces {:media-type "text/html"}})
               {:produces [{:media-type #{TEXT_HTML}}]})))

    (testing "just-string"
      (is (= (coercer {:produces "text/html"})
             {:produces [{:media-type #{TEXT_HTML}}]})))))
