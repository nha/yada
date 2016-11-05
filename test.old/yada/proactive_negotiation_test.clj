(ns yada.proactive-negotiation-test
  (:require
   [clojure.test :refer :all]
   [clojure.string :as str]
   [schema.test :as st]
   [clojure.core.async :as a]
   [yada.test :refer [response-for]]
   [yada.yada :refer [yada]]
   [yada.yada :as yada]))

;;(map :charset (get-in (yada/as-resource "Hello World!") [:methods :get :produces]))

#_(response-for
 {:produces [{:media-type "text/plain"
              :charset "utf-8"
              :language #{"en" "en-GB" "default"}
              }]
  :methods
  {:get {:response #'foo
         }}}

 :get "/" {:headers {"accept-language" "en"}})

#_(yada/resource {:produces {:media-type "text/html;q=0.8" :language "en"} :methods {:get {:response "foo"}}})


#_(yada/resource {:methods {:get {:response "foo"}}})
