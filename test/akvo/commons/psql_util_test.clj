(ns akvo.commons.psql-util-test
  (:require
   [akvo.commons.psql-util]
   [clj-time.coerce :as c]
   [clj-time.core :as t]
   [clojure.data :refer [diff]]
   [clojure.test :refer [deftest testing is]]
   [clojure.java.jdbc :as jdbc])
  (:import
   java.sql.Timestamp
   org.postgresql.util.PGobject))


(defn- val->PGobject->val
  "Testing helper fn that pack and unpack via PGobject."
  [v]
  (jdbc/result-set-read-column (jdbc/sql-value v)
                               nil
                               nil))


(deftest basics

  (testing "nil"
    (is (= nil
           (val->PGobject->val  nil))))

  (testing "Empty sting"
    (is (= ""
           (val->PGobject->val ""))))

  (testing "String"
    (is (= "str"
           (val->PGobject->val "str"))))

  (testing "Number"
    (is (= 0
           (val->PGobject->val 0))))

  (testing "Boolean"
    (is (= true
           (val->PGobject->val true)))))


(deftest json

  (testing "hash map"
    (is (= {}
           (val->PGobject->val {})))

    (is (= {"key" "value"}
           (val->PGobject->val {"key" "value"})))

    (is (= {"key" {"key" "value"}}
           (val->PGobject->val {"key" {"key" "value"}}))))

  (testing "vector"
    (is (= []
           (val->PGobject->val [])))

    (is (= ["first" "second"]
           (val->PGobject->val ["first" "second"])))))


(deftest dates

  (testing "Correct date"
    (let [t0 (t/date-time 1986 10 14 4 3 27 456)
          t1 (val->PGobject->val t0)]
      (is (t/equal? t0 t1))))

  (testing "Same timestamps (long)"

    (let [t0 (c/to-long (t/date-time 1981 3 25))
          t1 (val->PGobject->val t0)]
      (is (= t0 t1)))))
