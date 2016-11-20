(ns yada.spec
  (:require [clojure.spec :as s]))

(s/def :yada/method-token
  (->
   (s/and string?
          ;; By convention, standardized methods are defined in all-uppercase
          ;; US-ASCII letters. -- https://tools.ietf.org/html/rfc7231#section-4
          #(re-matches #"[A-Z]+" %))

   (s/with-gen
     (fn [] (s/gen #{"ACL" "BASELINE-CONTROL" "BIND" "BREW" "CHECKIN" "CHECKOUT" "CONNECT" "COPY" "DELETE" "GET" "HEAD" "LABEL" "LINK" "LOCK" "MERGE" "MKACTIVITY" "MKCALENDAR" "MKCOL" "MKREDIRECTREF" "MKWORKSPACE" "MOVE" "OPTIONS" "ORDERPATCH" "PATCH" "POST" "PRI" "PROPFIND" "PROPPATCH" "PUT" "REBIND" "REPORT" "SEARCH" "TRACE" "UNBIND" "UNCHECKOUT" "UNLINK" "UNLOCK" "UPDATE" "UPDATEREDIRECTREF" "VERSION-CONTROL"})))))


(defn validate [data spec error]
  (when-not (s/valid? spec data)
    (println (format "%s: %s" error (s/explain-str spec data)))
    (throw
     (ex-info (format "%s: %s" error (s/explain-str spec data))
              {:explain (s/explain-data spec data)
               :data data})))
  data)
