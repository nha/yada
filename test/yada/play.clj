(ns yada.play
  (:require [clojure.test :as t]
            [cats.core :as m]
            [manifold.deferred :as d]
            [cats.labs.manifold :as mf]
            [cats.context :as ctx]
            [cats.monad.maybe :as maybe]
            [cats.monad.either :as either]
            cats.builtin))

(m/mappend (maybe/just [1 2 3]) (maybe/nothing) (maybe/just [4 5 6]))

(ctx/with-context maybe/context
  (m/fapply
   (maybe/just (fn [x] x))
   (maybe/just [1 2 3])
   (maybe/just [4 5 6])))


(m/>=> either/context  maybe/context )

(m/bind (either/right 4)
        (fn [a] (m/bind
                 (either/right 2)
                 (fn [b] (+ a b))
                 )))

(ctx/with-context either/context
  (m/mlet [a (either/right 10)
           b (either/right 8)
           c (either/right 8)]
          (+ a b c)
          ))

(defn service-available? [ctx] (either/right (assoc ctx :available true)))
(defn process-request-body [ctx] (either/right (assoc ctx :body "Hello World!")))
(defn get-properties [ctx] (either/right (assoc ctx :properties {})))
;;(defn get-properties [ctx] (d/future (either/right (assoc ctx :properties {}))))
(defn authenticate [ctx] (either/left :no-auth))
(defn authorize [ctx] (either/right ctx))

(m/foldm #(%2 %1)
         {:init :start}
         [service-available?
          process-request-body
          get-properties
          ;;   authenticate
          authorize])



(m/lift-m)

((m/lift-m 2 +) (maybe/just 1) (maybe/nothing))

((m/lift-m 2 +) (maybe/nothing) [0 2 4] )


(defn async-call
  "A function that emulates some asynchronous call."
  [n]
  (d/future
    (println "---> sending request" n)
    (Thread/sleep n)
    (println "<--- receiving request" n)
    n))

(defn sync-call
  "A function that emulates some asynchronous call."
  [n]
  (println "---> sending sync request" n)
  (println "<--- receiving sync request" n)
  n)



(m/fapply)


(m/foldl (fn [ctx f] (f ctx))
         {}
         [(fn [ctx] (future ctx))
          (fn [ctx] ctx)])

(m/fapply (fn ))

(m/->= (async-call 10)
       ((fn [x] 20))
       (async-call)
       (async-call)

       )

(m/->= (sync-call 10)

 )




;; set: M

;; Algebraic Structures

;; An algebraic structures is a set + functions (finitary operations) + axioms that hold on function application (e.g. associativity, communitivity)

;; Notation. [set, operations, axioms]

;; magma: [set, binary op, closure]: M x M -> M, x·y
;; semigroup: [set, binary op, closure + associativity]: (x·y)·z = x·(y·z)
;; monoid: [set, binary op, closure + associativity + identity]

;; Group

;; D[set, binary op, closure + associativity + identity + invertibility]
