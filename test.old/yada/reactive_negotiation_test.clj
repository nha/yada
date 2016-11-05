(ns yada.reactive-negotiation-test
  (:require
   [clojure.test :refer :all]
   [schema.test :as st]
   [yada.test :refer [response-for]]
   [yada.yada :as yada]))

#_(response-for
 {:produces [{:media-type "text/plain"
              :charset "utf-8"
              :language #{"en" "en-GB" "default"}}]
  :methods
  {:get {:response (fn [ctx] "foo")}}}
 :get "/" {:headers {"accept-language" "en-US"}})
