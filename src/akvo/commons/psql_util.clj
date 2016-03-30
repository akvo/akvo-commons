(ns akvo.commons.psql-util
  "Require this and Postgres knows how to speak json, jsonb & timestamps."
  (:require
   [cheshire.core :as json]
   [clj-time.coerce :as c]
   [clj-time.jdbc]
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


;; ;; From Timestamp
;; (extend-protocol jdbc/IResultSetReadColumn
;;   Timestamp
;;   (result-set-read-column [ts _ _]
;;     (-> ts
;;         c/from-sql-time
;;         c/to-long)))
