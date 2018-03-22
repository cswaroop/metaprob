(ns metaprob.builtin-impl-test
  (:require [clojure.test :refer :all]
            [metaprob.trace :as trace]
            [metaprob.builtin-impl :refer :all]))

(deftest last-1
  (testing "Last element of a metaprob list"
    (is (= (metaprob-last (seq-to-metaprob-list '(1 2 3)))
           3))))

(deftest append-1
  (testing "Concatenate two metaprob lists"
    (let [l1 (seq-to-metaprob-list '(1 2 3))
          l2 (seq-to-metaprob-list '(7 8 9))]
      (is (= (metaprob-last (append l1 l2))
             9)))))

(deftest list-1
  (testing "Assemble and access a mp-list"
    (is (= (trace/metaprob-first (metaprob-list 4 5 6))
           4))))

(deftest list-2
  (testing "Assemble and access a mp-list"
    (is (= (trace/metaprob-first (trace/metaprob-rest (metaprob-list 4 5 6)))
           5))))

(deftest tuple2list
  (testing "Convert metaprob tuple to metaprob list"
    (let [v [5 7 11 13]
          t v]  ;was: (seq-to-metaprob-tuple v)
      (is (trace/metaprob-tuple? t))
      (let [l (tuple-to-list t)]
        (is (trace/metaprob-pair? l))
        (let [v2 (vec (trace/metaprob-list-to-seq l))]
          (is (= v2 v)))))))

(deftest list2tuple
  (testing "Convert metaprob list to metaprob tuple"
    (let [v '(5 7 11 13)
          t (seq-to-metaprob-list v)]
      (is (trace/metaprob-pair? t))
      (let [l (list-to-tuple t)]
        (is (trace/metaprob-tuple? l))
        (let [v2 (vec (trace/metaprob-tuple-to-seq l))]
          (is (= v2 v)))))))

(deftest tag-capture
  (testing "capture- and retrieve-tag-address smoke test"
    (let [root (trace/new-trace "root")
          q (capture-tag-address root root root)
          a (metaprob-list "this" "that")
          r (resolve-tag-address (trace/pair q a))
          o2 (metaprob-nth r 2)]
      (is (trace/trace? o2))
      (trace/trace-set o2 "value")
      (is (= (trace/trace-get o2) "value"))
      (is (= (trace/trace-get (trace/lookup root a)) "value"))
      (is (= (trace/trace-get root a) "value")))))

;; Probprog stuff

(deftest foreign-probprog
  (testing "create and call a foreign-probprog"
    (let [pp (make-foreign-probprog "pp" (fn [x] (+ x 1)))]
      (is (= (generate-foreign pp [6]) 7)))))

;(deftest basic-query
;  (testing "query a foreign-probprog"
;    (let [pp (make-foreign-probprog "pp" (fn [x] (+ x 1)))]
;      (let [[answer score] (impl/mini-query pp [7] nil nil nil)]
;        (is (= score 0))
;        (is (= answer 8))))))
;
;(deftest lift-and-query
;  (testing "can we lift a probprog and then query it"
;    (let [qq (impl/make-foreign-probprog "qq" (fn [argseq i t o]
;                                             [(+ (metaprob-nth argseq 0) (metaprob-nth argseq 1))
;                                              50]))
;          lifted (make-lifted-probprog "lifted" qq)]
;      (let [[answer score] (impl/mini-query lifted [7 8] nil nil nil)]
;        (is (= answer 15))
;        (is (= score 50))))))

(deftest reification-1
  (testing "Does a probprog appear to be a mutable-trace?"
    (let [pp (make-foreign-probprog "pp" (fn [x] (+ x 1)))]
      (is (= (trace/trace-get pp) "prob prog")))))

(deftest length-2
  (testing "length smoke test"
    (is (= (trace/length (seq-to-metaprob-list [1 2 3 4])) 4))))

(deftest range-1
  (testing "range smoke test"
    (let [r (metaprob-range 5)]
      (is (= (trace/length r) 5))
      (is (= (count (trace/metaprob-list-to-seq r)) 5))
      (is (= (trace/metaprob-first r) 0))
      (is (= (metaprob-last r) 4)))))

;; addresses-of

(deftest addresses-of-1
  (testing "Smoke test addresses-of"
    (let [tree (trace/trace-from-map
                {"x" (trace/trace-from-map {"a" (trace/new-trace 1)
                                            "b" (trace/new-trace 2)
                                            "c" (trace/empty-trace)})
                 "y" (trace/new-trace "d")})
          sites (addresses-of tree)]
      (is (= (trace/length sites) 3)))))

;; match-bind

;; (deftest match-bind-1
;;   (testing "match-bind smoke"
;;     (let [env (make-env ... what a pain in the ass ...)]
;;       (match-bind (from-clojure '[a b])
;;                   [1 2]
;;                   env)
;;       (is (= (env-lookup env "a") 1))
;;       (is (= (env-lookup env "b") 2)))))

;; Does list s contain element x?

(defn xmetaprob-list-contains? [s x]
  (if (trace/empty-trace? s)
    false
    (if (= x (trace/metaprob-first s))
      true
      (metaprob-list-contains? (trace/metaprob-rest s) x))))


(deftest list-contains-1
  (testing "smoke test metaprob-list-contains"
    (is (metaprob-list-contains? (seq-to-metaprob-list '(3 5 7))
                                   5))))

(deftest list-contains-2
  (testing "smoke test metaprob-list-contains"
    (is (not (metaprob-list-contains? (seq-to-metaprob-list '(3 5 7))
                                        11)))))

(deftest set-difference-1
  (testing "smoke test set-difference"
    (let [a (seq-to-metaprob-list '(3 5 7))
          b (seq-to-metaprob-list '(5 7 11 13))]
      (is (metaprob-list-contains? a 5) "5 in a")
      (let [a-b (set-difference a b)
            b-a (set-difference b a)]
        (is (metaprob-list-contains? a-b 3) "3 in a-b")
        (is (metaprob-list-contains? b-a 13) "13 in b-a")
        (is (not (metaprob-list-contains? a-b 7)) "7 not in a-b")
        (is (not (metaprob-list-contains? b-a 7)) "7 not in b-a")))))

;(deftest hairy-key-1
;  (testing "does it work to use a probprog name as a trace"
;    (let [pp (program [x] x)
;          pt pp
;          key (trace/trace-get pt "name")
;          tr (trace/trace-from-map {key (trace/new-trace 17)})]
;      (is (= (trace/trace-get tr key) 17)))))

(deftest addresses-of-1
  (testing "addresses-of (addresses-of)"
    (let [tr (trace/trace-from-map {"a" (trace/new-trace 17)
                                    "b" (trace/new-trace 31)
                                    "c" (trace/trace-from-map {"d" (trace/new-trace 71)})})
          sites (trace/metaprob-sequence-to-seq (addresses-of tr))
          vals  (map (fn [site] (trace/trace-get tr site)) sites)
          has? (fn [val] (some (fn [x] (= x val)) vals))]
      (has? 17)
      (has? 71))))