(ns yada.console.subs
    (:require-macros [reagent.ratom :refer [reaction]])
    (:require [re-frame.core :as re-frame]))

(re-frame/register-sub
 :db
 (fn [db _]
   (reaction @db)))

(re-frame/register-sub
 :active-panel
 (fn [db _]
   (reaction (:active-panel @db))))

(re-frame/register-sub
 :requests
 (fn [db _]
   (reaction (:requests @db))))

(re-frame/register-sub
 :active-request
 (fn [db _]
   (reaction (:active-request @db))))

(re-frame/register-sub
 :animation
 (fn [db _]
   (reaction (:animation @db))))

(re-frame/register-sub
 :cards
 (fn [db [_ id]]
   (reaction (get-in @db [:cards id]))))

(re-frame/register-sub
 :active-card
 (fn [db _]
   (println "subs: active-card reaction!")
   (reaction (:active-card @db))))
