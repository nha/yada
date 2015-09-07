;; Copyright © 2015, JUXT LTD.

(ns yada.dev.console
  (:require
   [bidi.bidi :refer (RouteProvider)]
   [bidi.ring :refer (files resources-maybe)]
   [com.stuartsierra.component :refer (using)]
   [modular.component.co-dependency :refer (co-using)]
   [clojure.java.io :as io]
   [schema.core :as s]
   [yada.yada :as yada :refer [yada]]))

(defrecord Console [config *router]
  RouteProvider
  (routes [component]
    ["/"
     [["console/home" (-> "console/resources/static/index.html" io/file (yada {:id ::index}))]
      ["react/react.min.js" (-> "cljsjs/production/react.min.inc.js" io/resource yada)]
      ["cljs" (files {:dir "target/cljs"})]

      ;; Customized css
      ["material.min.css" (-> "console/resources/static/material.min.css" io/file yada)]
      ["fonts.css" (-> "console/resources/static/fonts.css" io/file yada)]
      ["mdl.woff2" (-> "console/resources/static/mdl.woff2" io/file yada)]

      ["mdl/" (resources-maybe {:prefix "META-INF/resources/webjars/material-design-lite/1.0.2/"})]

      ]]))

(defn new-console [config]
  (-> (map->Console config)
      (co-using [:router])))
