(ns yada.dev.user-guide
  (:require
   [bidi.bidi :refer (tag RouteProvider path-for alts)]
   [bidi.ring :refer (redirect)]
   [com.stuartsierra.component :refer (using Lifecycle)]
   [tangrammer.component.co-dependency :refer (co-using)]
   [markdown.core :refer (md-to-html-string)]
   [modular.template :as template :refer (render-template)]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.xml :refer (emit-element parse)]
   [clojure.walk :refer (postwalk)]))

(defn enclose [^String s]
  (format "<div>%s</div>" s))

(defn xml-parse [^String s]
  (parse (io/input-stream (.getBytes s))))

(defn get-source []
  (xml-parse (enclose (md-to-html-string
                       (slurp (io/resource "user-guide.md"))))))

(defn post-process [xml]
  (postwalk
   (fn [{:keys [tag attrs] :as el}]
     (cond (= tag :example)
           {:tag :div :attrs {:class "example"} :content (concat
                                                          [{:tag :h3 :content [(:name attrs)]}]
                                                          [(xml-parse (enclose (md-to-html-string (slurp (io/resource (format "examples/pre/%s.md" (:name attrs)))))))])}
           :otherwise el))
   xml))

(defn body [templater]
  (let [xbody (get-source)]
    (render-template
     templater
     "templates/page.html.mustache"
     {:content
      (with-out-str
        (emit-element
         (post-process xbody)))})))

(defrecord UserGuide [*router templater]
  Lifecycle
  (start [component] (assoc component :start-time (java.util.Date.)))
  (stop [component] component)
  RouteProvider
  (routes [component]
    ["/user-guide.html"
     (fn [req] {:status 200
                :body (body templater)})]))

(defn new-user-guide [& {:as opts}]
  (-> (->> opts
           (merge {})
           map->UserGuide)
      (using [:templater])
      (co-using [:router])))
