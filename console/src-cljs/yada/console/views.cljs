(ns yada.console.views
  (:require
   [clojure.string :as str]
   [juxt.mdl.layout :as lo]
   [juxt.mdl.navigation :as nav]
   [juxt.mdl.tables :as t]
   [re-frame.core :as re-frame]
   [yada.console.routes :refer [path-for]]))

;; --------------------
(defn header []
  [lo/header
   [lo/header-row
    [lo/title "yada console"]
    [lo/spacer]
    [nav/nav [{:label "navlink 1" :href "/nav1"}
              {:label "navlink 2" :href "/nav2"}
              {:label "navlink 3" :href "/nav3"}]]]])

(defn home-panel []
  (let [requests (re-frame/subscribe [:requests])]
    [lo/layout
     [header]
     [lo/drawer
      [lo/title "title"]
      [nav/nav-lso [{:label "navlink 1" :href "/nav1"}
                    {:label "navlink 2" :href "/nav2"}
                    {:label "navlink 3" :href "/nav3"}]]]
     [lo/content
      [:div.page-content
       [:h3 "Requests"]
       [t/table {:cols ["Id" "Date" "URI" "Method" "Status"]}
        (for [[id {:keys [date uri method status]}] @requests]
          [[:a {:href (path-for :request :id id)} id]
           date uri
           (str method)
           status
           ])]]]]))

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
(defmethod panels :home-panel [] [home-panel])
(defmethod panels :request-panel [] [request-panel])
(defmethod panels :default [] [:div [:p "Unknown panel"]])

(defn main-panel []
  (let [active-panel (re-frame/subscribe [:active-panel])]
    (fn []
      (panels @active-panel))))
