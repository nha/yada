(ns yada.console.db)

(def default-db
  {
   :cards {:resources {:title "Resources"
                       :supporting-text "Resources that have been accessed, showing recent requests, state changes."
                       :actions ["Show"]}

           :errors {:title "Errors"
                    :supporting-text "Client and server errors that have been detected."
                    :actions ["Show"]}

           :routing {:title "Routing"
                     :supporting-text "Routes"
                     :actions ["Show"]}

           :performance {:title "Performance"
                         :supporting-text "Statistics gathered to show overall performance, with drill-downs to identify performance issues."
                         :actions ["Summary" "Detailed"]}

           :documentation {:title "Documentation"
                           :supporting-text "Help and documentation on yada"
                           :actions ["Show"]}}})
