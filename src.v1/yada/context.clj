;; Copyright © 2014-2016, JUXT LTD.

(ns yada.context)

(defrecord Response [])

(defn exists?
  "We assume every resource exists unless it says otherwise, with an
  explicit exists? entry in its properties."
  [ctx]
  (let [props (:properties ctx)]
    (if (contains? props :exists?)
      (:exists? props)
      true)))

;; Convenience functions, allowing us to encapsulate the context
;; structure.
(defn content-type [ctx]
  (get-in ctx [:response :produces :media-type :name]))

(defn charset [ctx]
  (get-in ctx [:response :produces :charset :alias]))

(defn language [ctx]
  (apply str (interpose "-" (get-in ctx [:response :produces :language :language]))))

(defn url-info [ctx handler & [options]]
  (if-let [uri-info (:uri-info ctx)]
    (uri-info handler options)
    (throw (ex-info "Context does not contain a :uri-info entry" {:keys (keys ctx)}))))

(def path-for (comp :path url-info))
(def host-for (comp :host url-info))
(def scheme-for (comp :scheme url-info))
(def href-for (comp :href url-info))
(def url-for (comp :uri url-info))
