(ns bifurcan.list-tests
  (:require
   [clojure.test :refer :all])
  (:import
   [io.lacuna.bifurcan
    List]))

;; Access some private constants in the Java implementation, so some
;; tests can be parameterized based upon those values.

(def listnode-class io.lacuna.bifurcan.nodes.ListNodes)
(def listnode-max-branches-field (.getDeclaredField listnode-class "MAX_BRANCHES"))
(.setAccessible listnode-max-branches-field true)
(def max-branches (.get listnode-max-branches-field listnode-class))

(defn same-seq [a b]
  (= (seq a) (seq b)))

(deftest github-issue-19
  (let [b max-branches
        n (+ (* b b b) (- (* b b)) b)
        r1 (range 2)
        r2 (range 2 (+ 2 n))
        r3 (concat r1 r2)
        r4 (subvec (vec r3) 1 10)
        l1 (.concat (List.) (List/from r1))
        l2 (.concat (List.) (List/from r2))
        l3 (.concat l1 l2)]
    (is (= true (same-seq l1 r1)))
    (is (= true (same-seq l2 r2)))
    (is (= true (same-seq l3 r3)))
    (let [l4 (.slice l3 1 10)]
      (is (= true (same-seq l4 r4))))))
