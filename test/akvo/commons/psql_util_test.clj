(ns akvo.commons.psql-util-test
  (:require
   [akvo.commons.psql-util]
   [clj-time.coerce :as c]
   [clj-time.core :as t]
   [clojure.data :refer [diff]]
   [clojure.test :refer [deftest testing is]]
   [clojure.java.jdbc :as jdbc])
  (:import
   ;; java.sql.Timestamp
   org.postgresql.util.PGobject))


(defn- equal?
  "No diff."
  [v1 v2]
  (let [[a b c] (diff v1 v2)]
    (and (nil? a)
         (nil? b)
         (= v1 c))))

(defn- boomerang
  "Pack and unpack via PGobject."
  [v]
  (jdbc/result-set-read-column (jdbc/sql-value v)
                               nil
                               nil))

(deftest roundtrip
  (testing "nil"

    (is (equal? nil
                (boomerang nil))))

  (testing "Empty sting"
    (is (equal? ""
                (boomerang ""))))

  (testing "hash map"
    (is (equal? {}
                (boomerang {})))

    (is (equal? {"key" "value"}
                (boomerang {"key" "value"})))

    (is (equal? {"key" {"key" "value"}}
                (boomerang {"key" {"key" "value"}}))))

  (testing "vector"
    (is (equal? []
                (boomerang [])))

    (is (equal? ["first" "second"]
                (boomerang ["first" "second"]))))

  ;; (testing "org.joda.time.DateTime"
  ;;   (let [t0 (t/date-time 1986 10 14 4 3 27 456)
  ;;         t1 (boomerang t0)]
  ;;     (prn (type t1))
  ;;     (is (t/equal? t0 t1))))

  ;; (testing "timestamp"
  ;;   (let [t0 (c/to-long (t/date-time 1981 3 25))
  ;;         t1 (boomerang t0)]
  ;;     (is (= t0 t1))))


  ;; (testing "timestamp"
  ;;   (let [t0 (c/to-long (t/date-time 1981 3 25))
  ;;         o (doto (PGobject.)
  ;;             (.setType "timestamp")
  ;;             (.setValue (str t0)))
  ;;         t1 (jdbc/result-set-read-column o nil nil)]
  ;;     (is (= t0 t1))))
  )
