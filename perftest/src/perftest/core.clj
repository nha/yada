(ns perftest.core
  (:require [clj-http.client :as client]
            [clj-gatling.core :refer [run-simulation]]))

(defn s []
  (client/put "http://localhost:8090/perftest/3"
              {:throw-exceptions false
               :multipart [{:name "000001"
                            :content "file"
                            :encoding "UTF-8"
                            :mime-type "text/plain"}]}))

;;(s)

(defn open-frontpage [callback context]
  (let [was-call-succesful? (s)]
    (callback was-call-succesful? context)))

(run-simulation
 [{:name "sequentially try each endpoint"
   :requests [{:name "perftest 2"
               :fn open-frontpage}]}]
 40
 {:requests 1000})




