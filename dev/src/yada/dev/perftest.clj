(ns yada.dev.perftest
  (:require
   [bidi.bidi :refer [RouteProvider]]))

(defrecord PerfTestResources []
  RouteProvider
  (routes [component]
    ["/perftest/"
     [["1" (fn [req] {:status 200 :body "Hello Perf Tester!"})]]]))

(defn new-perftest-resources []
  (map->PerfTestResources {}))
