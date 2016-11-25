;; Copyright © 2014-2016, JUXT LTD.

(ns yada.resource-coercion-test
  (:require
   [clojure.test :refer :all]
   [yada.resource :refer [coerce-to-resource-map]]
   [clojure.spec :as s]))

(deftest resource-map-coercion-test
  (is (=
       {:yada.resource/methods [{:yada/method-token "GET"} {:yada/method-token "POST"}]}
       (coerce-to-resource-map {:yada.resource/methods (array-map :get {} :post {})}))))

#_(s/explain-str :yada/resource {:yada.resource/authentication-schemes
                               [{:yada.resource/scheme "Basic"
                                 :yada.resource/realm "default"
                                 :yada.resource/authenticate (fn [ctx] ctx)}]
                               :yada.resource/methods [{:yada/method-token "GET"}]})

#_(coerce-to-resource-map {:yada.resource/access-control
                         {:yada.resource/realms []}
                         :yada.resource/methods (array-map :get {} :post {})})
