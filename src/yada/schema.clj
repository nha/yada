;; Copyright © 2015, JUXT LTD.

(ns yada.schema
  (:require
   [clojure.walk :refer [postwalk]]
   [yada.media-type :as mt]
   [schema.core :as s]
   [schema.coerce :as sc]
   [yada.charset :refer [to-charset-map]])
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

(def RepresentationSetMappings
  {[RepresentationSet] as-vector
   RepresentationSet as-representation-set
   #{MediaTypeMap} as-set
   MediaTypeMap as-media-type
   #{CharsetMap} as-set
   CharsetMap to-charset-map})

(defprotocol FunctionCoercion
  (as-fn [_] "Coerce to function"))

(extend-protocol FunctionCoercion
  clojure.lang.Fn
  (as-fn [f] f)
  Object
  (as-fn [o] (constantly o)))

(s/defschema HandlerFunction
  (s/=> s/Any Context))

(s/defschema PropertiesResultSchema
  {(s/optional-key :last-modified) s/Inst
   (s/optional-key :version) s/Any})

(s/defschema PropertiesHandlerFunction
  (s/=> PropertiesResultSchema Context))

(s/defschema PropertiesSchema
  {(s/optional-key :properties) PropertiesHandlerFunction})

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

(def MethodsSchemaMappings
  (merge {MethodSchema as-method-map
          HandlerFunction as-fn}
         RepresentationSetMappings))

(def ResourceSchema
  (merge PropertiesSchema
         ProducesSchema
         ConsumesSchema
         MethodsSchema))

(def ResourceSchemaMappings
  (merge {PropertiesHandlerFunction as-fn}
         RepresentationSetMappings
         MethodsSchemaMappings))

(def resource-coercer (sc/coercer ResourceSchema ResourceSchemaMappings))

;; ------

(s/defschema Representation
  (s/constrained
   {:media-type MediaTypeMap
    (s/optional-key :charset) CharsetMap
    (s/optional-key :language) String
    (s/optional-key :encoding) String}
   not-empty))

(s/defschema UnrolledProducesSchema
  {(s/optional-key :produces) [Representation]})

(s/defschema UnrolledConsumesSchema
  {(s/optional-key :consumes) [Representation]})

(s/defschema UnrolledMethodSchema
  (merge {:handler HandlerFunction}
         UnrolledProducesSchema
         UnrolledConsumesSchema))

(s/defschema UnrolledMethodsSchema
  {:methods {s/Keyword UnrolledMethodSchema}})

(def UnrolledResourceSchema
  (merge PropertiesSchema
         UnrolledProducesSchema
         UnrolledConsumesSchema
         UnrolledMethodsSchema))

(defn- representation-seq
  "Return a sequence of all possible individual representations."
  [reps]
  (for [rep reps
        media-type (or (:media-type rep) [nil])
        charset (or (:charset rep) [nil])
        language (or (:language rep) [nil])
        encoding (or (:encoding rep) [nil])]
    (merge
     (when media-type {:media-type media-type})
     (when charset {:charset charset})
     (when language {:language language})
     (when encoding {:encoding encoding}))))

(defn- unroll-representation-sets
  [resource]
  (->> resource
       (postwalk
        (fn [x]
          (if (and (vector? x) (= (first x) :produces))
            [:produces (representation-seq (second x))]
            x)))
       (postwalk
        (fn [x]
          (if (and (vector? x) (= (first x) :consumes))
            [:consumes (representation-seq (second x))]
            x)))
       ))

(def unrolled-resource-coercer
  (comp (sc/coercer UnrolledResourceSchema {})
        unroll-representation-sets
        resource-coercer))
