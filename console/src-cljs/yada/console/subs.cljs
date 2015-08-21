(ns yada.console.subs
    (:require-macros [reagent.ratom :refer [reaction]])
    (:require [re-frame.core :as re-frame]))

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
