(ns yada.play-test
  (:require
   [clojure.test :refer :all]
   [manifold.deferred :as d]
   [clojure.test.check :as tc]
   [clojure.test.check.generators :as gen]
   [clojure.test.check.properties :as prop]))

(def chain
  [(fn [ctx]
     (future (update ctx :marks conj :a)))
   (fn [ctx]
     (update ctx :marks conj :b))
   (fn [ctx]
     (future (update ctx :marks conj :c)))])

(defn synchronous-executor [chain]
  (fn [ctx]
    (reduce
     (fn [ctx f]
       (let [ctx (f ctx)]
         (cond-> ctx (d/deferrable? ctx) deref)))
     ctx chain)))

(deftest synchronous-executor-test
  (is
   (=
    (let [executor (synchronous-executor chain)]
      (executor {:marks []}))
    {:marks [:a :b :c]})))

(defn manifold-asynchronous-executor [chain]
  (fn [ctx]
    (apply d/chain ctx chain)))

(deftest manifold-executor-test
  (is
   (=
    (let [executor (manifold-asynchronous-executor chain)]
      @(executor {:marks []}))
    {:marks [:a :b :c]})))


(defn ascending?
  "clojure.core/sorted? doesn't do what we might expect, so we write our
  own function"
  [coll]
  (every? (fn [[a b]] (<= a b))
          (partition 2 1 coll)))

(def property
  (prop/for-all [v (gen/vector gen/int)]
    (let [s (sort v)]
      (and (= (count v) (count s))
           (ascending? s)))))

;; test our property
(deftest qc
  (tc/quick-check 100 property))


(gen/generate (gen/vector gen/boolean 3))
