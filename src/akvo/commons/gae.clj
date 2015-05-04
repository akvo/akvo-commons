;  Copyright (C) 2014 Stichting Akvo (Akvo Foundation)
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
  (:require [taoensso.timbre :as timbre :refer (info debugf error)])
  (:import java.util.Date java.io.IOException
    [com.google.appengine.tools.remoteapi RemoteApiInstaller RemoteApiOptions]
    [com.google.appengine.api.datastore DatastoreServiceFactory Entity Query
     Query$FilterOperator Query$CompositeFilterOperator Query$FilterPredicate
     PreparedQuery FetchOptions FetchOptions$Builder KeyFactory Key]))

(defmacro with-datastore
  "Evaluates body in a try expression with ds bound to a remote datastore object
   built according to spec. The spec is a map consisting of
     :server - The remote server (default \"localhost\")
     :port - The remote port (default 8888)
     :email - The email to use for authentication (default \"test@example.com\")
     :password - The password to use for authentication (default \"\")"
  [[ds spec] & body]
  `(let [options# (doto (RemoteApiOptions.)
                    (.server (:server ~spec "localhost")
                             (:port ~spec 8888))
                    (.credentials (:email ~spec "test@example.com")
                                  (:password ~spec "")))
         installer# (RemoteApiInstaller.)]
     (.install installer# options#)
     (try
       (let [~ds (DatastoreServiceFactory/getDatastoreService)]
         ~@body)
       (finally
         (.uninstall installer#)))))

(defn ^:deprecated get-options
  "Returns a RemoteApiOptions object. Deprecated, use with-datastore instead"
  [server usr pwd]
  (debugf "Creating RemoteApiOptions - server: %s - user: %s" server usr)
  (doto
    (RemoteApiOptions.)
    (.server server 443)
    (.credentials usr pwd)))

(defn ^:deprecated get-installer
  "Returns a RemoteApiInstaller object. Deprecated, use with-datastore instead"
  [opts]
  (try
    (doto
      (RemoteApiInstaller.)
      (.install opts))
    (catch IllegalStateException e
      (info (.getMessage e)))
    (catch IOException e
      (error e "Error installing options"))))

(defn get-fetch-options
  "Returns the fetch options for a PreparedQuery"
  ([]
    (FetchOptions$Builder/withDefaults))
  ([size]
    (FetchOptions$Builder/withChunkSize size))
  ([size cursor]
    (.startCursor (FetchOptions$Builder/withLimit size) cursor)))

(defn ^:deprecated get-ds
  "Returns an instance of a DatastoreService. Deprecated, use with-datastore instead"
  []
  (DatastoreServiceFactory/getDatastoreService))

(defn get-filter
  "Helper function that returns a FilterPredicate based on a property"
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
  [server usr pwd entity-name props]
  (debugf "Creating new entity - entity-name: %s - props: %s" entity-name props)
  (let [opts (get-options server usr pwd)
        installer (get-installer opts)
        ds (get-ds)
        entity (Entity. ^String entity-name)
        ts (Date.)]
    (doseq [k (keys props)]
      (.setProperty entity (name k) (props k)))
    (.setProperty entity "createdDateTime" ts)
    (.setProperty entity "lastUpdateDateTime" ts)
    (let [k (.put ds entity)]
      (.uninstall installer)
      k)))
