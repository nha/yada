(ns yada.console.handlers
  (:require [re-frame.core :as re-frame]
            [thi.ng.tweeny.core :as tweeny]
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

(defn animate [n k]
  (when (pos? n)
    (re-frame/dispatch [:animation-frame k])
    (.requestAnimationFrame js/window #(animate (dec n) k))))

(re-frame/register-handler
 :animation-frame
 (fn [db [_ k]]
   (-> db
       (update-in [k :top] rest)
       (update-in [k :left] rest)
       (update-in [k :width] rest)
       (update-in [k :height] rest))))

(re-frame/register-handler
 :animate-card
 (fn [db [_ {:keys [a b dur]}]]
   (animate 1 12)
   db))

(re-frame/register-handler
 :card-click
 (fn [db [_ card-id]]
   (let [node (get-in db [:cards card-id :node])
         from-rect (.getBoundingClientRect node)
         to-rect (.. node -parentElement -parentElement -parentElement getBoundingClientRect)

         tw (tweeny/mix-cosine)
         r (range 0 1 0.1)]
     (println "making card active")
     (println "from-rect")
     (.dir js/console from-rect)
     (println "to-rect")
     (.dir js/console to-rect)
     (animate 10 :active-card)
     (assoc db
            :active-card {:id card-id
                          :top (for [i r]
                                 (tw (.-top from-rect) 0 i))
                          :left (for [i r]
                                  (tw (.-left from-rect) (.-left to-rect) i))
                          :width (for [i r]
                                   (tw (.-width from-rect) (.-width to-rect) i))
                          :height (for [i r]
                                    (tw (.-height from-rect) (.-height to-rect) i))}))))

(re-frame/register-handler
 :card-did-mount
 (fn [db [_ id node]]
   (println "Card did mount:" id (.getBoundingClientRect node))
   (assoc-in db [:cards id :node] node)))

(re-frame/register-handler
 :close-card
 (fn [db _]
   (println "closing card")
   (assoc db :active-card {:id :none})))
