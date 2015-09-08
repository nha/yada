(ns yada.console.views
  (:require
   [cljs.pprint :as pp]
   [thi.ng.tweeny.core :as tweeny]
   [clojure.string :as str]
   [juxt.mdl.layout :as lo]
   [juxt.mdl.navigation :as nav]
   [juxt.mdl.tables :as t]
   [re-frame.core :as re-frame]
   [reagent.core :as reagent]
   [yada.console.routes :refer [path-for]]))

(enable-console-print!)

;; --------------------
(defn header []
  [lo/header
   [lo/header-row
    [lo/title "yada console"]
    [lo/spacer]
    [nav/nav [{:label "navlink 1" :href "/nav1"}
              {:label "navlink 2" :href "/nav2"}
              {:label "navlink 3" :href "/nav3"}]]]])

(defn grid [& content]
  [:div.mdl-grid {:style {:position "relative"}} content])


(defn cell [width content]
  (fn []
    (let [elk (keyword (str "div.mdl-cell.mdl-cell--" width "-col"))]
      [elk content])))

(defn card-did-mount [id]
  (fn [this]
    (re-frame/dispatch-sync [:card-did-mount id (reagent/dom-node this)])))

;;(def ctg (aget js/React "addons" "CSSTransitionGroup"))

(defn card-view [id]
  (let [card (re-frame/subscribe [:cards id])
        active-card (re-frame/subscribe [:active-card])]
    (fn []
      (let [active (= id (:id @active-card))]
        [:div.demo-card-wide.mdl-card.mdl-shadow--2dp
         (merge {}  (when active {:class "active-card"}))
         #_{:style (if active
                     {:z-index 10
                    :position :absolute
                    :top (first (:top @active-card))
                    :left (first (:left @active-card)) #_(str (:current-value @animation) "px")
                    :width (first (:width @active-card)) #_(str (+ 680 (* 2 (- 534 (:current-value @animation)))) "px")
                    :height (first (:height @active-card)) #_(str (+ 100 (- 534 (:current-value @animation))) "px")
                    }
                   {})}
         [:div.mdl-card__title
          {:style
           (merge {}
                  (let [background (:background card)]
                    (if background
                      {:background (cond (string? background)
                                         (str "url('" background "') center / cover;")
                                         (keyword? background)
                                         (name keyword))}
                      {:background "#002"})))
           }
          [:h2.mdl-card__title-text (:title @card)]]
         [:div.mdl-card__supporting-text (:supporting-text @card)]

         #_(:content @card)

         [:div.mdl-card__actions.mdl-card--border
          (if (not active)
            (let [a "Show" #_(:actions @card)]
              [:a.mdl-button.mdl-button--colored.mdl-js-button.mdl-js-ripple-effect
               {:on-click (fn [ev] (re-frame/dispatch-sync [:card-click id]))}
               a]))]

         [:div.mdl-card__menu
          (if active
            [:button.mdl-button.mdl-button--icon.mdl-js-button.mdl-js-ripple-effect
             {:on-click (fn [ev]
                          (println "close please!")
                          (re-frame/dispatch-sync
                           [:close-card]))}
             [:i.material-icons "dashboard"]]
            [:button.mdl-button.mdl-button--icon.mdl-js-button.mdl-js-ripple-effect
             {:on-click (fn [ev]
                          (re-frame/dispatch-sync
                           [:card-click id]))}
             [:i.material-icons "open_in_new"]])]]))))

(defn card [id]
  (reagent/create-class
   {:reagent-render (card-view id)
    :component-did-mount (card-did-mount id)}))

(defn home-panel []
  (let [requests (re-frame/subscribe [:requests])]
    (fn []
      [lo/layout
       [header]
       [lo/drawer
        [lo/title "title"]
        [nav/nav-lso [{:label "navlink 1" :href "/nav1"}
                      {:label "navlink 2" :href "/nav2"}
                      {:label "navlink 3" :href "/nav3"}]]]
       [lo/content
        [:div.page-content
         [grid
          [cell 4 [card :resources]]
          [cell 4 [card :errors]]
          [cell 4 [card :routing]]
          [cell 4 [card :performance]]
          [cell 4 [card :documentation]]]]]])))

(defn home-panel-did-mount [this]
  (let [grid (.querySelector (reagent/dom-node this) ".mdl-grid")]
    (.dir js/console (.getBoundingClientRect grid))))

(defn home-panel-component []
  (reagent/create-class {:reagent-render home-panel
                         :component-did-mount home-panel-did-mount}))

(defn method->str [k]
  (str/upper-case (name k)))

(defn request-panel []
  (let [req (re-frame/subscribe [:active-request])]
    (fn []
      (let [{:keys [date uri method status]} @req]
        [lo/layout
         [header]
         [lo/drawer
          [lo/title "title"]
          [nav/nav-lso [{:label "navlink 1" :href "/nav1"}
                        {:label "navlink 2" :href "/nav2"}
                        {:label "navlink 3" :href "/nav3"}]]]
         [lo/content
          [:div.page-content
           [:h3 "Request"]
           [:p "keys: " (str @req)]
           [:p "curl: "]
           [:pre (method->str method) " " uri]
           [:h4 "Resource"]
           [:h4 "Representations"]
           [t/table {:cols ["Media-type" "Charset" "Encoding" "Language"]}
            #_[[{} "text/html" "UTF-8" "identity" "en-GB"]
               [{} "application/json" "UTF-8" "identity" nil]
               [{} "application/json" "UTF-16" "identity" nil]
               [{} "application/json" "UTF-32" "identity" nil]]
            ]
           [:div [:a {:href (path-for :home)} "Requests"]]
           ]
          ]])))
  )

;; --------------------
(defmulti panels identity)
(defmethod panels :home-panel [] [home-panel-component])
(defmethod panels :request-panel [] [request-panel])
(defmethod panels :default [] [:div [:p "Unknown panel"]])

(defn main-panel []
  (let [active-panel (re-frame/subscribe [:active-panel])
        db (re-frame/subscribe [:db])]
    (fn []
      [:div
       (panels @active-panel)
       [:h4 "Database"]
       [:pre (with-out-str (pp/pprint @db))]])))

;; (defn render []
;;   (set! (.x (.rotation mesh)) (+ (.x (.rotation mesh)) 0.01))
;;   (set! (.y (.rotation mesh)) (+ (.y (.rotation mesh)) 0.02))
;;   (.render renderer scene camera))

;; (defn animate []
;;   (.requestAnimationFrame js/window animate)
;;   (render))

;; (animate)

;; (getBoundingClientRect)

(defn foo [from to]
  (let [f (tweeny/mix-cosine)]
    (for [i (range 0 1 0.1)]
      (f from to i))))
