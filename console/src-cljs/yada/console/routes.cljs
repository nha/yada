(ns yada.console.routes
  (:require [clojure.set :refer [rename-keys]]
            [bidi.bidi :as bidi]
            [pushy.core :as pushy]
            [goog.string :as gstring]
            [re-frame.core :as re-frame]
            [yada.xhr :as xhr]))

(enable-console-print!)

(def routes
  ["/console/" {"home" :home
                ["request/" :id] :request
                }])

(defn- dispatch-route [match]
  (case (:handler match)
    :home
    (let [panel-name (keyword (str (name (:handler match)) "-panel"))]
      (re-frame/dispatch [:set-active-panel panel-name]))

    :request
    (let [uri (gstring/format "http://localhost:8090/journal/%s" (-> match :route-params :id))]
      (do (println "AJAX request:" uri)
          (xhr/GET uri)


          ))

    #_(re-frame/dispatch [:set-active-panel :request-panel (-> match :route-params :id)])))

(defn app-routes []
  (pushy/start! (pushy/pushy dispatch-route (partial bidi/match-route routes))))

(defn path-for [tag & args]
  (apply bidi/path-for routes tag args))
