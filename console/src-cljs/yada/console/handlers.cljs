(ns yada.console.handlers
  (:require [re-frame.core :as re-frame]
            [thi.ng.tweeny.core :as tweeny]
            [yada.console.db :as db]))

(re-frame/register-handler
 :initialize-db
 (fn  [_ _] db/default-db))

(re-frame/register-handler
 :card-click
 (fn [db [_ card-id]]
   (println "card-click! card-id = " card-id)
   (assoc db :active-card {:id card-id})))

(re-frame/register-handler
 :close-card
 (fn [db _]
   (println "close-card, back to dashboard")
   (assoc db :active-card {:id :none})))
