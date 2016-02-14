(ns perftest.core
  (:require [clj-http.client :as client]
            [clj-gatling.core :refer [run-simulation]]))

(g/run-simulation [{:name     "sequentially try each endpoint"
                    :requests [{:name "Post mirakl CSV to queue"
                                :fn   (partial post-mirakl-csv api-root)}]}]
                  1
                  {:requests 1000})

(defn s []
  (client/post "http://localhost:8090/api/mirakl/payment-voucher"
               {:throw-exceptions false
                :multipart [{:name "000001"
                             :content "file"
                             :encoding "UTF-8"
                             :mime-type "text/plain"}]
                :headers {"X-Request-Id" "1234abcd"}}))

(run-simulation
 [{:name "Localhost test scenario"
   :fn s}] 100)


