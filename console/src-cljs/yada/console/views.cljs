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

(defn card-view [id title background supporting-text actions content]
  (fn []
    [:div.demo-card-wide.mdl-card.mdl-shadow--2dp
     [:div.mdl-card__title
      {:style
       (merge {}
              (if background
                {:background (cond (string? background)
                                   (str "url('" background "') center / cover;")
                                   (keyword? background)
                                   (name keyword))}
                {:background "#002"}))
       :on-click (fn [ev] (re-frame/dispatch-sync [:card-click id]))}
      [:h2.mdl-card__title-text title]]
     [:div.mdl-card__supporting-text supporting-text]

     content

     [:div.mdl-card__actions.mdl-card--border
      (for [a actions]
        [:a.mdl-button.mdl-button--colored.mdl-js-button.mdl-js-ripple-effect a])]

     [:div.mdl-card__menu
      [:button.mdl-button.mdl-button--icon.mdl-js-button.mdl-js-ripple-effect
       [:i.material-icons "share"]]]]))

(defn card-did-mount [id]
  (fn [this]
    (re-frame/dispatch-sync [:card-did-mount id (reagent/dom-node this)])))

(defn card [id title background supporting-text actions & content]
  (reagent/create-class
   {:reagent-render (card-view id title background supporting-text actions content)
    :component-did-mount (card-did-mount id)}))

(defn card-db-view [id]
  (let [card (re-frame/subscribe [:cards id])]
    (fn []
      (println "card-db-view:" @card)
      [:div.demo-card-wide.mdl-card.mdl-shadow--2dp
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
         :on-click (fn [ev] (re-frame/dispatch-sync [:card-click id]))}
        [:h2.mdl-card__title-text (:title @card)]]
       [:div.mdl-card__supporting-text (:supporting-text @card)]

       #_(:content @card)

       [:div.mdl-card__actions.mdl-card--border
        (for [a (:actions @card)]
          [:a.mdl-button.mdl-button--colored.mdl-js-button.mdl-js-ripple-effect a])]

       [:div.mdl-card__menu
        [:button.mdl-button.mdl-button--icon.mdl-js-button.mdl-js-ripple-effect
         [:i.material-icons "share"]]]])))

(defn card-db [id]
  (reagent/create-class
   {:reagent-render (card-db-view id)
    :component-did-mount (card-did-mount id)}))

#_(defn cell-active [content]
  (let [elk (keyword (str "div.mdl-cell.mdl-cell--12-col"))]
    [elk content]))

#_(defn card-active [title background supporting-text actions & content]
  (let [animation (re-frame/subscribe [:animation])]
    (fn []
      [:div.demo-card-wide.mdl-card.mdl-shadow--6dp.card-big
       {:on-click (fn [ev]
                    (println "click on card, dispatch event")
                    (re-frame/dispatch-sync
                     [:animate-card {:a 534 :b 10 :dur 10}]))
        :style {:z-index 10
                :position :absolute
                :top "15"
                ;; These start at these values, then the width transitions
                ;; to the full-width, left transitions to 0, height
                ;; transitions to full-height
                :left (str (:current-value @animation) "px")
                :width (str (+ 680 (* 2 (- 534 (:current-value @animation)))) "px")
                ;;width: "510px"

                :height (str (+ 100 (- 534 (:current-value @animation))) "px")
                }}
       [:div.mdl-card__title
        {:style (merge {} (if background
                            {:background (cond (string? background)
                                               (str "url('" background "') center / cover;")
                                               (keyword? background)
                                               (name keyword))}
                            {:background "#000"}))}
        [:h2.mdl-card__title-text title]]
       [:div.mdl-card__supporting-text supporting-text]

       content

       [:div.mdl-card__actions.mdl-card--border
        (for [a actions]
          [:a.mdl-button.mdl-button--colored.mdl-js-button.mdl-js-ripple-effect a])]

       [:div.mdl-card__menu
        [:button.mdl-button.mdl-button--icon.mdl-js-button.mdl-js-ripple-effect
         [:i.material-icons "share"]]]])))

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
          [cell 4 [card-db :resources]]
          [cell 4 [card-db :errors]]
          [cell 4 [card-db :routing]]
          [cell 4 [card-db :performance]]
          [cell 4 [card-db :documentation]]]

         #_[:h3 "Requests"]
         #_[t/table {:cols ["Id" "Date" "URI" "Method" "Status"]}
            (for [[id {:keys [date uri method status]}] @requests]
              [[:a {:href (path-for :request :id id)} id]
               date uri
               (str method)
               status
               ])]]]])))

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
  (for [i (range 0 1 0.1)]
    ((tweeny/mix-cosine) from to i)))
