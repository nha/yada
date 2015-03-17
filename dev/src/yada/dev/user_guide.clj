(ns yada.dev.user-guide
  (:require
   [bidi.bidi :refer (tag RouteProvider alts)]
   [bidi.ring :refer (redirect)]
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.pprint :refer (pprint *print-right-margin*)]
   [clojure.string :as str]
   [clojure.walk :refer (postwalk)]
   [clojure.xml :refer (parse)]
   [com.stuartsierra.component :refer (using Lifecycle)]
   [hiccup.core :refer (h) :rename {h escape-html}]
   [markdown.core :refer (md-to-html-string)]
   [modular.bidi :refer (path-for)]
   [modular.template :as template :refer (render-template)]
   [modular.component.co-dependency :refer (co-using)]
   [yada.dev.examples :refer (resource-map get-path-args request)]))

(defn emit-element
  ;; An alternative emit-element that doesn't cause newlines to be
  ;; inserted around punctuation.
  [e]
  (if (instance? String e)
    (print e)
    (do
      (print (str "<" (name (:tag e))))
      (when (:attrs e)
	(doseq [attr (:attrs e)]
	  (print (str " " (name (key attr))
                      "='"
                      (let [v (val attr)] (if (coll? v) (apply str v) v))
                      "'"))))
      (if (:content e)
	(do
	  (print ">")
          (if (instance? String (:content e))
            (print (:content e))
            (doseq [c (:content e)]
              (emit-element c)))
	  (print (str "</" (name (:tag e)) ">")))
	(print "/>")))))

(defn basename [r]
  (last (str/split (.getName (type r)) #"\.")))

(defn enclose [^String s]
  (format "<div>%s</div>" s))

(defn xml-parse [^String s]
  (parse (io/input-stream (.getBytes s))))

(defn get-source []
  (xml-parse (enclose (md-to-html-string
                       (slurp (io/resource "user-guide.md"))))))

(defn title [s]
  (letfn [(lower [x]
            (if (#{"as" "and" "of" "for"}
                 (str/lower-case x)) (str/lower-case x) x))]
    (->> (re-seq #"[A-Z][a-z]*" s)
         (map lower)
         (str/join " "))))

(defn chapter [c]
  (str/replace (apply str c) #"\s+" ""))

(defn ->meth
  [m]
  (case m
    :get "GET"
    :put "PUT"
    :delete "DELETE"
    :post "POST"))

(defn example-instance [user-guide example]
  (when-let [v (find-var (symbol "yada.dev.examples" (str "map->" (namespace-munge example))))]
    (v user-guide)))

(defn post-process-example [user-guide example xml]
  (when xml
    (let [ex (example-instance user-guide example)
          url (apply path-for @(:*router user-guide) (keyword (basename ex)) (get-path-args ex))
          {:keys [method headers]} (request ex)]

      (postwalk
       (fn [{:keys [tag attrs content] :as el}]
         (cond


           (= tag :resource-map)
           {:tag :div
            :content [{:tag :pre
                       :content [{:tag :code
                                  :attrs {:class "clojure"}
                                  :content [(escape-html
                                             (str/trim
                                              (with-out-str
                                                (binding [*print-right-margin* 52]
                                                  (pprint (resource-map ex))))))]}]}]}

           (= tag :request)
           {:tag :div
            :content [{:tag :pre
                       :content [{:tag :code
                                  :attrs {:class "http"}
                                  :content [(str (->meth method) (format " %s HTTP/1.1" url)
                                                 (apply str (for [[k v] headers] (format "\n%s: %s" k v))))]}]}
                      ]}

           (= tag :response)
           {:tag :div
            :attrs {:id (format "response-%s" (basename ex))}
            :content [{:tag :p
                       :content [{:tag :button
                                  :attrs {:class "btn btn-primary"
                                          :type "button"
                                          :onClick (format "tryIt(\"%s\",\"%s\",\"%s\",%s)"
                                                           (->meth method)
                                                           url
                                                           (basename ex)
                                                           (json/encode headers))}
                                  :content ["Try it"]}
                                 " "
                                 {:tag :button
                                  :attrs {:class "btn"
                                          :type "button"
                                          :onClick (format "clearIt(\"%s\")"
                                                           (basename ex))}

                                  :content ["Reset"]}
                                 ]}
                      {:tag :table
                       :attrs {:class "table"}
                       :content [{:tag :tbody
                                  :content [{:tag :tr
                                             :content [{:tag :td :content ["Status"]}
                                                       {:tag :td :attrs {:class "status"} :content [""]}]}
                                            {:tag :tr
                                             :content [{:tag :td :content ["Headers"]}
                                                       {:tag :td :attrs {:class "headers"} :content [""]}]}
                                            {:tag :tr
                                             :content [{:tag :td :content ["Body"]}
                                                       {:tag :td :content [{:tag :textarea
                                                                            :attrs {:class "body"}
                                                                            :content [""]}]}]}]}]}]}

           ;; Raise divs in paragraphs.
           (and (= tag :p) (= (count content) 1) (= (:tag (first content)) :div))
           (first content)

           :otherwise el))
       xml))))


(defn post-process-doc [user-guide xml]
  #_(pprint xml)
  (postwalk
   (fn [{:keys [tag attrs content] :as el}]
     (cond
       (= tag :h2)
       {:tag :div
        :content [{:tag :a :attrs {:name (chapter content)} :content []}
                  el]}

       (= tag :example)
       {:tag :div
        :attrs {:class "example"}
        :content
        (concat
         [{:tag :h3 :content [(title (:ref attrs))]}]
         (remove nil? [(post-process-example
                        user-guide
                        (:ref attrs)
                        (some-> (format "examples/pre/%s.md" (:ref attrs))
                                io/resource slurp md-to-html-string enclose xml-parse))]))}

       (= tag :include)
       {:tag :div
        :attrs {:class (:type attrs)}
        :content [{:tag :a :attrs {:name (:ref attrs)} :content []}
                  (some-> (format "includes/%s.md" (:ref attrs))
                          io/resource slurp md-to-html-string enclose xml-parse)]}

       ;; Raise divs in paragraphs.
       (and (= tag :p) (= (count content) 1) (= (:tag (first content)) :div))
       (first content)

       (= tag :code)
       (update-in el [:content] (fn [x] (map (fn [y] (if (string? y) (str/trim y) y)) x)))
       :otherwise el))
   xml))

(defn post-process-body
  "Some whitespace reduction"
  [s]
  (-> s
      (str/replace #"<p>\s*</p>" "")
      (str/replace #"(yada)" "<span class='yada'>yada</span>")
      #_(str/replace #"<pre>\s+" "<pre>")
      #_(str/replace #"\s+</pre>" "</pre>")
      #_(str/replace #"<code([^>]*)>\s+" (fn [[_ x]] (str "<code" x ">")))
      #_(str/replace #"\s+</code>" "</code>")))

(defn body [{:keys [*router templater] :as user-guide}]
  (let [xbody (get-source)]
    (render-template
     templater
     "templates/page.html.mustache"
     {:content
      (-> (with-out-str
            (emit-element
             (post-process-doc user-guide xbody)))
          post-process-body
          )})))

(defrecord UserGuide [*router templater]
  Lifecycle
  (start [component]
    (assoc component :start-time (java.util.Date.)))
  (stop [component] component)
  RouteProvider
  (routes [component]
    ["/user-guide.html"
     (-> (fn [req] {:status 200
                    :body (body component)})
         (tag ::user-guide))]))

(defn new-user-guide [& {:as opts}]
  (-> (->> opts
           (merge {})
           map->UserGuide)
      (using [:templater])
      (co-using [:router])))


#_(emit-element {:tag :div :attrs {:class "foo" :onClick "tryIt('GET')"}})
