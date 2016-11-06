(ns yada.spec
  (:require
   clojure.string
   clojure.test.check
   clojure.test.check.generators
   [clojure.string :as str]
   [clojure.test :as t]
   [clojure.spec :as s]
   [clojure.spec.gen :as gen]
   [yada.util :refer (http-token OWS)]
   yada.media-type))

(s/def :yada/id keyword?)
(s/def :yada/description string?)
(s/def :yada/summary string?)

(s/def :yada/method-token
  (->
   (s/and string?
          ;; By convention, standardized methods are defined in all-uppercase
          ;; US-ASCII letters. -- https://tools.ietf.org/html/rfc7231#section-4
          #(re-matches #"[A-Z]+" %))

   (s/with-gen
     (fn [] (s/gen #{"ACL" "BASELINE-CONTROL" "BIND" "BREW" "CHECKIN" "CHECKOUT" "CONNECT" "COPY" "DELETE" "GET" "HEAD" "LABEL" "LINK" "LOCK" "MERGE" "MKACTIVITY" "MKCALENDAR" "MKCOL" "MKREDIRECTREF" "MKWORKSPACE" "MOVE" "OPTIONS" "ORDERPATCH" "PATCH" "POST" "PRI" "PROPFIND" "PROPPATCH" "PUT" "REBIND" "REPORT" "SEARCH" "TRACE" "UNBIND" "UNCHECKOUT" "UNLINK" "UNLOCK" "UPDATE" "UPDATEREDIRECTREF" "VERSION-CONTROL"})))))

(def media-type-pattern
  (re-pattern (str "(" http-token ")"
                   "/"
                   "(" http-token ")"
                   "((?:" OWS ";" OWS http-token "=" http-token ")*)")))

(def media-type-pattern-no-subtype
  (re-pattern (str "(\\*)"
                   "((?:" OWS ";" OWS http-token "=" http-token ")*)")))

(defn string->media-type [s]
  (when s
    (let [g (rest (or (re-matches media-type-pattern s)
                      (concat (take 2 (re-matches media-type-pattern-no-subtype s))
                              ["*" (last (re-matches media-type-pattern-no-subtype s))])))
          params (into {} (map vec (map rest (re-seq (re-pattern (str ";" OWS "(" http-token ")=(" http-token ")"))
                                                     (last g)))))]
      #:yada.media-type {:name (str (first g) "/" (second g))
                         :type (first g)
                         :subtype (second g)
                         :parameters (dissoc params "q")
                         :quality (if-let [q (get params "q")]
                                    (try
                                      (Float/parseFloat q)
                                      (catch java.lang.NumberFormatException e
                                        (float 1.0)))
                                    (float 1.0))})))

(defn media-type-conformer [x]
  (cond (map? x) x
        (string? x) (merge {:yada.media-type/name x} (string->media-type x))
        :otherwise x))

(s/def :yada.media-type/name string?)
(s/def :yada.media-type/type string?)
(s/def :yada.media-type/subtype string?)
(s/def :yada.media-type/quality number?)
(s/def :yada.media-type/parameters map?)

(s/def :yada/charset string?)
(s/def :yada/encoding string?)
(s/def :yada/language string?)

(s/def :yada/media-type
  (s/and (s/with-gen
           (s/conformer media-type-conformer)
           (fn [] (s/gen (s/keys :req [:yada.media-type/name :yada.media-type/type :yada.media-type/subtype]
                                 :opt [:yada.media-type/parameters :yada.media-type/quality]))))
         (s/keys :req [:yada.media-type/name :yada.media-type/type :yada.media-type/subtype]
                 :opt [:yada.media-type/parameters :yada.media-type/quality])))

(defn representation-conformer [x]
  (cond
    (string? x) {:yada/media-type x}
    (map? x) x
    :otherwise :clojure.spec/invalid))

(def representation
  (s/and (s/with-gen (s/conformer representation-conformer)
           (fn [] (s/gen (s/keys :req [:yada/media-type]
                                 :opt [:yada/charset :yada/encoding :yada/language]))))
         (s/keys :req [:yada/media-type]
                 :opt [:yada/charset :yada/encoding :yada/language])))

(defn representation-set-conformer [x]
  (cond
    (string? x) [(representation-conformer {:yada/media-type x})]
    (vector? x) (mapv representation-conformer x)
    (map? x) [x]
    :otherwise x))

(def representation-set
  (s/with-gen
    (s/and
     (s/conformer representation-set-conformer)
     (s/coll-of representation))
    (fn [] (s/gen (s/coll-of representation)))))

(s/def :yada/produces representation-set)
(s/def :yada/consumes representation-set)

;; Ooh dangerous!
(s/def :yada/response
  (s/with-gen
    (s/fspec :args (s/cat :context (s/keys :req [:yada/method-token] :opt []))
             ;;:ret string?
             )
    (fn [] (s/gen #{(fn [_] nil)}))))

(s/def :yada/method
  (s/keys :req [:yada/method-token
             ;;   :yada/response
                ]
          :opt [:yada/produces
                :yada/consumes
                ]))

(defn method-conformer [x]
  (cond
    (map? x) x
    (vector? x) (let [[k v] x]
                  (merge v {:yada/method-token (cond (string? k) k
                                                     (keyword? k) (clojure.string/upper-case (name k))
                                                     :otherwise :clojure.spec/invalid)}))
    :otherwise :clojure.spec/invalid))

(s/def :yada/methods
  (s/* (s/and
        (s/with-gen (s/conformer method-conformer)
          (fn [] (s/gen :yada/method)))
        :yada/method)))


#_(s/def :yada/access-control
    )

(s/def :yada/exists? boolean?)

(s/def :yada/properties
  (s/or :val (s/keys :opt [:yada/last-modified
                           :yada/version
                           :yada/exists?])
        :fn (s/fspec :args (s/cat :context (s/keys :req [:yada/method-token] :opt [])) ))
  )


(s/def :yada/resource
  (s/keys :req [:yada/methods]
          :opt [:yada/id
                :yada/description
                :yada/summary
                :yada/properties
;;                :yada/access-control
                ]
          ))

#_(s/conform :yada/produces "text/html")

#_(s/explain-str :yada/resource {:yada/methods [{:yada/method-token "GET"
                                               :yada/produces "text/html"
                                               :yada/response (fn [_] nil)}]})

#_(s/explain-str :yada/resource
               {:yada/methods
                {:get {:yada/produces "text/html"
                       :yada/response (fn [_] nil)}}})

;;(s/conform :yada/produces "text/html")

;;(gen/sample (s/gen :yada/resource))

;;(first (drop 3 (gen/sample (s/gen :yada/resource))))

#_(s/gen :yada/resource)


(s/def :yada/context (s/keys :req [:yada/method]))
