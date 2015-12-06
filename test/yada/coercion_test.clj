;; Copyright © 2015, JUXT LTD.

(ns yada.coercion-test
  (:require
   [clojure.test :refer :all]
   [juxt.iota :refer [given]]
   [yada.media-type :as mt]
   [schema.core :as s]
   [schema.coerce :as sc]
   [schema.utils :refer [error?]])
  (:import [yada.charset CharsetMap]
           [yada.media_type MediaTypeMap]
           [java.util Date]))

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

(def ProducesSchemaMappings
  {[RepresentationSet] as-vector
   RepresentationSet as-representation-set
   #{MediaTypeMap} as-set
   MediaTypeMap as-media-type})

(deftest produces-test
  (let [coercer (sc/coercer ProducesSchema ProducesSchemaMappings)]

    (testing "produces"
      (testing "empty spec. is an error"
        (is (error? (coercer {:produces {}}))))

      (testing "as-vector"
        (is (= (coercer {:produces {:media-type #{HTML}}})
               {:produces [{:media-type #{HTML}}]})))

      (testing "to-set"
        (is (= (coercer {:produces {:media-type HTML}})
               {:produces [{:media-type #{HTML}}]})))

      (testing "string"
        (is (= (coercer {:produces {:media-type "text/html"}})
               {:produces [{:media-type #{HTML}}]})))

      (testing "just-string"
        (is (= (coercer {:produces "text/html"})
               {:produces [{:media-type #{HTML}}]})))

      (testing "string-set"
        (is (= (coercer {:produces #{"text/html" "application/json"}})
               {:produces [{:media-type #{HTML JSON}}]}))))))

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

(defn invoke-with-ctx [f] (f {}))

(def MethodsSchemaMappings (merge {MethodSchema as-method-map
                                   HandlerFunction as-fn}
                                  ProducesSchemaMappings))

(deftest methods-test
  (let [coercer (sc/coercer MethodsSchema MethodsSchemaMappings)]
    (testing "methods"
      (testing "string constant"
        (given (coercer {:methods {:get {:handler "Hello World!"}}})
               identity :- MethodsSchema
               [:methods :get :handler invoke-with-ctx] := "Hello World!"))

      (testing "implied handler"
        (given (coercer {:methods {:get (fn [ctx] "Hello World!")}})
               identity :- MethodsSchema
               [:methods :get :handler invoke-with-ctx] := "Hello World!"))

      (testing "both"
        (given (coercer {:methods {:get "Hello World!"}})
               identity :- MethodsSchema
               [:methods :get :handler invoke-with-ctx] := "Hello World!"
               [:methods :get :produces first :media-type first :name] := "text/plain"))

      (testing "produces inside method"
        (given (coercer {:methods {:get {:handler "Hello World!"
                                         :produces "text/plain"}}})
               identity :- MethodsSchema
               [:methods :get :handler invoke-with-ctx] := "Hello World!"
               ;;[:methods :get] := "foo"
               )))))

(def ResourceSchema
  (merge ProducesSchema ConsumesSchema MethodsSchema))

(def ResourceSchemaMappings
  (merge ProducesSchemaMappings ; will work for ConsumesSchema too
         MethodsSchemaMappings))

(deftest combo-test
  (let [coercer (sc/coercer ResourceSchema ResourceSchemaMappings)]
    (testing "produces works at both levels"
      (given (coercer {:produces "text/html"
                       :methods {:get {:produces "text/html"
                                       :handler "Hello World!"}}})
             identity :- ResourceSchema))))


