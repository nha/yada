(ns yada.console.handlers
    (:require [re-frame.core :as re-frame]
              [yada.console.db :as db]))

(re-frame/register-handler
 :initialize-db
 (fn  [_ _]
   db/default-db))

(re-frame/register-handler
 :set-active-panel
 (fn [db [_ active-panel id]]
   (case active-panel
     :home-panel
     (assoc db :active-panel active-panel)
     :request-panel
     (->
      db
      (assoc :active-panel active-panel)
      (assoc :active-request (get (:requests db) id))))))
