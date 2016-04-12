(ns akvo.commons.psql-util
  "Require this and Postgres knows how to speak json, jsonb."
  (:require
   [cheshire.core :as json]
   [clojure.java.jdbc :as jdbc])
  (:import
   java.sql.Timestamp
   org.postgresql.util.PGobject))


;; To jsonb

(defn val->jsonb-pgobj
  [v]
  (doto (PGobject.)
    (.setType "jsonb")
    (.setValue (json/generate-string v))))

(extend-protocol jdbc/ISQLValue
  clojure.lang.IPersistentMap
  (sql-value [v] (val->jsonb-pgobj v))

  clojure.lang.IPersistentVector
  (sql-value [v] (val->jsonb-pgobj v)))


;; From json & jsonb

(defn pgobj->val
  [^PGobject pgobj]
  (let [t (.getType pgobj)
        v (.getValue pgobj)]
    (case t
      "json"  (json/parse-string v)
      "jsonb" (json/parse-string v)
      :else   v)))

(extend-protocol jdbc/IResultSetReadColumn
  PGobject
  (result-set-read-column [pgobj _ _]
    (pgobj->val pgobj)))
