;; Copyright © 2015, JUXT LTD.

(ns yada.schema
  (:require
   [yada.media-type :as mt]
   [schema.core :as s])
  (:import
   [yada.charset CharsetMap]
   [yada.media_type MediaTypeMap]))

(s/defschema Context
  {})

(s/defschema RepresentationSet
  (s/constrained
   {:media-type #{MediaTypeMap}
    (s/optional-key :charset) #{CharsetMap}
    (s/optional-key :language) #{String}
    (s/optional-key :encoding) #{String}}
   not-empty))

(s/defschema ProducesSchema
  {(s/optional-key :produces) [RepresentationSet]})

(s/defschema ConsumesSchema
  {(s/optional-key :consumes) [RepresentationSet]})

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
  clojure.lang.PersistentHashSet
  (as-representation-set [s] {:media-type s})
  clojure.lang.APersistentMap
  (as-representation-set [m] m)
  String
  (as-representation-set [s] {:media-type s}))

(def HTML (mt/string->media-type "text/html"))
(def JSON (mt/string->media-type "application/json"))

(def RepresentationSetMappings
  {[RepresentationSet] as-vector
   RepresentationSet as-representation-set
   #{MediaTypeMap} as-set
   MediaTypeMap as-media-type})

(s/defschema HandlerFunction
  (s/=> s/Any Context))

(s/defschema MethodSchema
  (merge {:handler HandlerFunction}
         ProducesSchema
         ConsumesSchema))

(s/defschema MethodsSchema
  {:methods {s/Keyword MethodSchema}})

(defprotocol MethodSchemaCoercion
  (as-method-map [_] "Coerce to MethodSchema"))

(extend-protocol MethodSchemaCoercion
  clojure.lang.APersistentMap
  (as-method-map [m] m)
  String
  (as-method-map [o] {:handler o
                      :produces "text/plain"})
  Object
  (as-method-map [o] {:handler o
                      :produces "application/octet-stream"}))

(defprotocol FunctionCoercion
  (as-fn [_] "Coerce to function"))

(extend-protocol FunctionCoercion
  clojure.lang.Fn
  (as-fn [f] f)
  Object
  (as-fn [o] (constantly o)))

(def MethodsSchemaMappings (merge {MethodSchema as-method-map
                                   HandlerFunction as-fn}
                                  RepresentationSetMappings))

(def ResourceSchema
  (merge ProducesSchema ConsumesSchema MethodsSchema))

(def ResourceSchemaMappings
  (merge RepresentationSetMappings MethodsSchemaMappings))

