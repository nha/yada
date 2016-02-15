(ns perftest.core
  (:require
   [byte-streams :as b]
   [clojure.java.io :as io]
   [clj-http.client :as client]
   [clj-gatling.core :refer [run-simulation]]
   [manifold.stream :as s]))

(defn to-chunks [s size]
  (b/to-byte-buffers s {:chunk-size size}))

(defn assemble [acc item]
  (letfn [(join [item1 item2]
            (-> item1
                (assoc :type (case [(:type item1) (:type item2)]
                               [:partial :partial-completion] :part))
                (assoc :content (str (:content item1) (:content item2)))))]
    (case (:type item)
      :preamble (conj acc item)
      :part (conj acc item)
      :partial (conj acc item)
      :partial-completion (update-in acc [(dec (count acc))] join item)
      :end (conj acc item))))

(defn slurp-byte-array [res]
  (let [baos (java.io.ByteArrayOutputStream.)]
    (io/copy (io/input-stream res) baos)
    (.toByteArray baos)))

(defn hit-multi
  [{:keys [chunk-size window-size]} callback context]
  (let [{:keys [boundary window-size chunk-size] :as spec}
        {:boundary   "----WebKitFormBoundaryZ3oJB7WHOBmOjrEi"
         :chunk-size chunk-size :window-size window-size}
        chunks (to-chunks (slurp-byte-array (io/resource "yada/multipart-1")) chunk-size)]
    (let [parts (->> chunks
                     s/->source
                     (parse-multipart boundary window-size chunk-size)
                     (s/map (fn [m] (-> m
                                        (assoc :content (if-let [b (:bytes m)] (String. b) "[no bytes]"))
                                        (dissoc :bytes :debug))))
                     s/stream->seq
                     (reduce assemble []))
          pass? (and (= [:part :part :part :end] (mapv :type parts))
                     (= "Content-Disposition: form-data; name=\"firstname\"\r\n\r\nJon" (get-in parts [0 :content]))
                     (= "Content-Disposition: form-data; name=\"surname\"\r\n\r\nPither" (get-in parts [1 :content]))
                     (= "Content-Disposition: form-data; name=\"phone\"\r\n\r\n1235" (get-in parts [2 :content])))]
      (callback pass? context))))
​
(g/run-simulation [{:name     "sequentially try each endpoint"
                    :requests [{:name "Post Hybris Captures on queue"
                                :fn   (partial hit-multi {:chunk-size 64 :window-size 160})}]}]
                  10
                  {:requests 10000})
