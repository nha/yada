(ns yada.console.core
    (:require [reagent.core :as reagent]
              [re-frame.core :as re-frame]
              [yada.console.handlers]
              [yada.console.subs]
              [yada.console.routes :as routes]
              [yada.console.views :as views]
              [yada.xhr :as xhr :refer (GET)]))

(defn mount-root []
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (routes/app-routes)
  (re-frame/dispatch-sync [:initialize-db])
  (xhr/request GET "http://localhost:8090/journal/"
               (fn [status body]
                 ;;(println "Status!" status)
                 ;;(println "Body:" body)

                 ))
  (mount-root))

(defn ^:export reload-hook []
  (.log js/console "My figwheel reload hook!")
  (reagent/force-update-all)
  ;; TODO: Do an update of the database
  )
