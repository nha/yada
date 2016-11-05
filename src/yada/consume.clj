;; Copyright © 2014-2016, JUXT LTD.

(ns yada.consume
  (:require
   [aleph.netty :refer [release]]
   [byte-streams :as b]
   [clojure.tools.logging :refer :all]
   [manifold.deferred :as d]
   [manifold.stream :as s]))

(defn save-to-file [ctx body-stream f]
  (let [fos (new java.io.FileOutputStream f false)
        fc (.getChannel fos)]
    (d/chain
     (s/reduce
      (fn [ctx buf]
        (let [niobuf (b/to-byte-buffer buf)]
          (.write fc niobuf))

        (update ctx :count (fnil inc 0)))
      ctx
      body-stream)
     (fn [ctx]
       (.close fc)
       (.close fos)
       (assoc-in ctx [:file] f)))))
