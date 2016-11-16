;; Copyright © 2014-2016, JUXT LTD.

(ns yada.resource-coercion-test
  (:require
   [clojure.test :refer :all]
   [yada.resource :refer [coerce-to-resource-map]]))

(deftest resource-map-coercion-test
  (is (=
       {:yada.resource/methods [{:yada/method-token "GET"} {:yada/method-token "POST"}]}
       (coerce-to-resource-map {:yada.resource/methods (array-map :get {} :post {})}))))
