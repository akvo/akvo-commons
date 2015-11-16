;  Copyright (C) 2013-2015 Stichting Akvo (Akvo Foundation)
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

(ns akvo.commons.config
  (:import java.io.File
    [com.google.apphosting.utils.config AppEngineWebXml AppEngineWebXmlReader AppEngineConfigException])
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.java.shell :as shell]
            [clojure.edn :as edn]
            [clojure.walk :refer (stringify-keys)]
            [me.raynes.fs :as fs]
            [com.stuartsierra.component :as component]))


(defonce configs (atom {}))

(defonce instance-alias (atom {}))

(defonce settings (atom {}))

(defonce s3bucket->app-id (atom {}))

(defn get-bucket-name
  "Extracts the bucket name from an upload domain url: https://akvoflow-1.s3.amazonaws.com => akvoflow-1"
  [url]
  (last (str/split (first (str/split url #"\.s3\.amazonaws\.com")) #"//")))

(defn get-instance-config
  "Returns a map with keys extracted from appengine-web.xml file"
  [^File file]
  (let [appengine-web (-> file .getAbsolutePath (AppEngineWebXmlReader. "") .readAppEngineWebXml)
        app-id (.getAppId appengine-web)
        props (.getSystemProperties appengine-web)
        app-alias (get props "alias")
        access-key (get props "aws_identifier")
        secret-key (get props "aws_secret_key")
        s3bucket (get props "s3bucket")
        apiKey (get props "restPrivateKey")
        domain (format "%s.appspot.com" app-id)
        cartodb-api-key (get props "cartodbApiKey")
        cartodb-sql-api (get props "cartodbSqlApi")]
    {:app-id app-id
     :alias app-alias
     :domain domain
     :access-key access-key
     :secret-key secret-key
     :s3bucket s3bucket
     :apiKey apiKey
     :cartodb-api-key cartodb-api-key
     :cartodb-sql-api cartodb-sql-api}))

(defn find-config
  "Find the config map for bucket-name or app-id"
  [app-id-or-bucket]
  (let [cfg @configs]
    (or (get cfg app-id-or-bucket)
        (get cfg (@s3bucket->app-id app-id-or-bucket))
        (throw (ex-info "No config found"
                        {:app-id-or-bucket app-id-or-bucket})))))

(defn get-criteria
  "Returns a map of upload configuration criteria"
  [bucket-name surveyId]
  (let [config (find-config bucket-name)]
    (stringify-keys (assoc config :surveyId surveyId))))

(defn get-domain
  "Returns the instance domain for a given base-url"
  [base-url]
  (last (str/split base-url #"//")))

(defn get-alias
  "Returns the instance alias for a given domain or
  the same domain if no alias if found"
  [base-url]
  (let [domain (get-domain base-url)]
    (get @instance-alias domain domain)))

(defn get-configs
  "Returns a map with all FLOW configuration"
  [path]
  (let [cfgs (map get-instance-config (fs/find-files path #"appengine-web.xml"))
        bucket-fn (fn [res k v]
                    (assoc res k (first v)))
        alias-fn (fn [res k v]
                   (assoc res k (:alias (first v))))]
    {:configs (reduce-kv bucket-fn {} (group-by :app-id cfgs))
     :s3bucket-appid (into {} (map (juxt :s3bucket :app-id) cfgs))
     :instance-alias (reduce-kv alias-fn {} (group-by :domain cfgs))}))

(defn set-config!
  "Resets the value of configs and alias maps based on the appengine-web.xml files"
  [path]
  (let [cfgs (get-configs path)]
    (reset! configs (:configs cfgs))
    (reset! s3bucket->app-id (:s3bucket-appid cfgs))
    (reset! instance-alias (:instance-alias cfgs))))

(defn set-settings!
  "Resets the value of settings reading the new values from the file path"
  [path]
  (reset! settings (-> path (io/file) (slurp) (edn/read-string))))

(defn reload [path]
  (let [pull (shell/with-sh-dir path (shell/sh "git" "pull"))]
    (when (zero? (pull :exit))
        (set-config! path))))


(defrecord FLOWConfig [path]

  component/Lifecycle

  (start [this]
    (let [cfgs (get-configs path)]
      (assoc this :configs (:configs cfgs)
                  :s3bucket-appid (:s3bucket-appid cfgs)
                  :instance-alias (:instance-alias cfgs))))
  (stop [this]
    (assoc this :configs nil
                :s3bucket-appid nil
                :instance-alias nil)))

(defn new-akvoflow-config [path]
  (map->FLOWConfig {:path path}))