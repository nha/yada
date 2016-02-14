(ns yada.dev.perftest
  (:require
   [bidi.bidi :refer [RouteProvider]]
   [yada.yada :as yada :refer [yada resource]]
   [schema.core :as s]))

(defrecord PerfTestResources []
  RouteProvider
  (routes [component]
    ["/perftest/"
     [["1" (fn [req] {:status 200 :body "Hello Perf Tester!"})]
      ["2" (yada "hi")]
      ["3" (yada
            (resource
             {:methods
              {:put
               {:parameters {:body {(s/required-key "000001") s/Str}}
                :consumes "multipart/form-data"
                :produces "text/plain"
                :response (fn [ctx] "Accepted")}}}))]
      ]]))

(defn new-perftest-resources []
  (map->PerfTestResources {}))
