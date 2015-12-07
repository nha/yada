;; Copyright © 2015, JUXT LTD.

(ns yada.schema-test
  (:require
   [clojure.test :refer :all]
   [juxt.iota :refer [given]]
   [yada.media-type :as mt]
   [yada.schema :refer :all]
   [schema.core :as s]
   [schema.coerce :as sc]
   [schema.utils :refer [error?]]))

(def HTML (mt/string->media-type "text/html"))
(def JSON (mt/string->media-type "application/json"))

(deftest produces-test
  (let [coercer (sc/coercer ProducesSchema RepresentationSetMappings)]
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

(defn invoke-with-ctx [f] (f {}))

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

(deftest combo-test
  (testing "produces works at both levels"
    (given (resource-coercer {:produces "text/html"
                              :methods {:get {:produces "text/html"
                                              :handler "Hello World!"}}})
           identity :- ResourceSchema)))


