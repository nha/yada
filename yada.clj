;; Copyright © 2014-2016, JUXT LTD.

(ns yada.yada
  (:require
   yada.context
   yada.handler
   [potemkin :refer (import-vars)]))

(import-vars
 [yada.handler handler accept-request]
 [yada.resource resource])
