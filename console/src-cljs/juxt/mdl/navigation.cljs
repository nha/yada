(ns juxt.mdl.navigation)

(defn nav [links]
  [:nav.mdl-navigation
   (for [{:keys [label href]} links]
     [:a.mdl-navigation__link {:href href} label])])

(defn nav-lso [links]
  [:nav.mdl-navigation.mdl-layout--large-screen-only
   (for [{:keys [label href]} links]
     [:a.mdl-navigation__link {:href href} label])])
