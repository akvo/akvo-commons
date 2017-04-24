;  Copyright (C) 2014-2015 Stichting Akvo (Akvo Foundation)
;
;  This file is part of Akvo FLOW.
;
;  Akvo FLOW is free software: you can redistribute it and modify it under the terms of
;  the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
;  either version 3 of the License or any later version.
;
;  Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
;  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
;  See the GNU Affero General Public License included below for more details.
;
;  The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.

(ns akvo.commons.gae
  (:import java.util.Date
    [com.google.appengine.tools.remoteapi RemoteApiInstaller RemoteApiOptions]
    [com.google.appengine.api.datastore DatastoreService DatastoreServiceFactory
                                        Entity KeyFactory Query$FilterOperator
                                        Query$FilterPredicate FetchOptions$Builder]))

(defn remote-options [spec]
  (let [hostname (:hostname spec "localhost")
        port (:port spec 8888)]
    (if (= "localhost" hostname)
      (doto (RemoteApiOptions.)
        (.server hostname port)
        (.useDevelopmentServerCredential))
      (doto (RemoteApiOptions.)
        (.server hostname port)
        (.useServiceAccountCredential ^String (:service-account-id spec)
                                      ^String (:private-key-file spec))))))

(defmacro with-datastore
  "Evaluates body in a try expression with ds bound to a remote datastore object
   built according to spec. The spec is a map consisting of
     :hostname - The remote server (default \"localhost\")
     :port - The remote port (default 8888)
     :service-account-id - The service account id (required for production envs)
     :private-key-file - The absolute path to private key in P12 format (required for production envs)"
  [[ds spec] & body]
  `(let [options# (remote-options ~spec)
         installer# (RemoteApiInstaller.)]
     (.install installer# options#)
     (try
       (let [~ds (DatastoreServiceFactory/getDatastoreService)]
         ~@body)
       (finally
         (.uninstall installer#)))))

(defn get-fetch-options
  "Returns the fetch options for a PreparedQuery"
  {:deprecated "0.4.2"}
  ([]
   (FetchOptions$Builder/withDefaults))
  ([size]
    (FetchOptions$Builder/withChunkSize size))
  ([size cursor]
    (.startCursor (FetchOptions$Builder/withLimit size) cursor)))

(defn get-filter
  "Helper function that returns a FilterPredicate based on a property"
  {:deprecated "0.4.2"}
  ([property value]
    (Query$FilterPredicate. property Query$FilterOperator/EQUAL value))
  ([property value operator]
    (cond
      (= operator :eq) (Query$FilterPredicate. property Query$FilterOperator/EQUAL value)
      (= operator :lt) (Query$FilterPredicate. property Query$FilterOperator/LESS_THAN value)
      (= operator :lte) (Query$FilterPredicate. property Query$FilterOperator/LESS_THAN_OR_EQUAL value)
      (= operator :gt) (Query$FilterPredicate. property Query$FilterOperator/GREATER_THAN value)
      (= operator :gte) (Query$FilterPredicate. property Query$FilterOperator/GREATER_THAN_OR_EQUAL value)
      (= operator :ne) (Query$FilterPredicate. property Query$FilterOperator/NOT_EQUAL value)
      (= operator :in) (Query$FilterPredicate. property Query$FilterOperator/IN value))))

(defn get-key
  [^String kind ^Long id]
  (KeyFactory/createKey kind id))

(defn put!
  "Creates a new Entity using Remote API"
  [^DatastoreService ds entity-name props]
  (let [entity (Entity. ^String entity-name)
        ts (Date.)]
    (doseq [k (keys props)]
      (.setProperty entity (name k) (props k)))
    (.setProperty entity "createdDateTime" ts)
    (.setProperty entity "lastUpdateDateTime" ts)
    (.put ds entity)))
