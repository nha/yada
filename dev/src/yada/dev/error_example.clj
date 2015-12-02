;; Copyright © 2015, JUXT LTD.

(ns yada.dev.error-example
  (:require
   [clojure.tools.logging :refer :all]
   [com.stuartsierra.component :refer [Lifecycle]]
   [bidi.bidi :refer [RouteProvider tag]]
   [ring.mock.request :refer [request]]
   [yada.swagger :refer [swaggered]]
   [yada.resources.journal-browser :refer [new-journal-browser-resources]]
   [yada.yada :as yada :refer [yada]]))

(defn hello-error [journal]
  (yada
   {:produces [{:media-type "text/html"}]
    :error-handler identity
    :journal journal
    :journal-browser-path "/journal/"
    :methods
    {:get {:handler (fn [ctx] (throw (ex-info "TODO: 123" {:foo :bar})))}}}))

(defrecord ErrorExample [journal error-resource]
  Lifecycle
  (start [component]
    (let [journal (atom {})
          _ (infof "Marker v")
          error-resource (hello-error journal)]
      (infof "Marker C")
      (dotimes [n 10]
        (do
          (infof "Marker (dotimes): %s" n)
          @(error-resource (request :get "/"))))

      (assoc component :journal journal :error-resource error-resource)))

  (stop [component] component)

  RouteProvider
  (routes [_]
    [""
     [
      ["/error" error-resource]
      ["/journal/" (new-journal-browser-resources :journal journal)]]]))

(defn new-error-example [& {:as opts}]
  (map->ErrorExample opts))
