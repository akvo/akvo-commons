;  Copyright (C) 2015 Stichting Akvo (Akvo Foundation)
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

(ns akvo.commons.gae.query
  (:refer-clojure :exclude (= not= < <= > >= and or key))
  (:import  [com.google.appengine.api.datastore
             Cursor
             DatastoreService
             Entity
             FetchOptions
             FetchOptions$Builder
             Key
             KeyFactory
             PreparedQuery
             Query
             Query$CompositeFilterOperator
             Query$FilterOperator
             Query$FilterPredicate
             Query$SortDirection]))

(def key Entity/KEY_RESERVED_PROPERTY)

;; Filters
(defn = [^String property value]
  (Query$FilterPredicate. property Query$FilterOperator/EQUAL value))

(defn not= [^String property value]
  (Query$FilterPredicate. property Query$FilterOperator/NOT_EQUAL value))

(defn < [^String property value]
  (Query$FilterPredicate. property Query$FilterOperator/LESS_THAN value))

(defn <= [^String property value]
  (Query$FilterPredicate. property Query$FilterOperator/LESS_THAN_OR_EQUAL value))

(defn > [^String property value]
  (Query$FilterPredicate. property Query$FilterOperator/GREATER_THAN value))

(defn >= [^String property value]
  (Query$FilterPredicate. property Query$FilterOperator/GREATER_THAN_OR_EQUAL value))

(defn in [^String property value]
  (Query$FilterPredicate. property Query$FilterOperator/IN value))

(defn and [& filters]
  (Query$CompositeFilterOperator/and filters))

(defn or [& filters]
  (Query$CompositeFilterOperator/or filters))

;; Sorting
(defn- dir [d]
  (condp clojure.core/= d
    :asc Query$SortDirection/ASCENDING
    :desc Query$SortDirection/DESCENDING
    (throw (ex-info (str "Invalid sort direction " d)
                    {:direction d}))))

(defn- add-sorts [^Query q sort-spec]
  (cond
    (string? sort-spec) (add-sorts q [[sort-spec :asc]])
    (string? (first sort-spec)) (add-sorts q [sort-spec])
    :else
    (doseq [[property direction] sort-spec]
      (.addSort q property (dir direction))))
  q)

;; Projections
;; https://cloud.google.com/appengine/docs/java/datastore/projectionqueries
(defn add-projections [^Query q ps]
  (doseq [[property class] ps]
    (.addProjection q property class))
  q)

(defn query
  "Create a query specified by the map
   :kind (required)
   :filter
   :sort-by - Either a property string, a vector [property :asc/desc]
              or a vector-of-vectors [[p1 dir] [p2 dir]]
   :keys-only? true/false (default false)
   :projections - A map of property -> class, ex: {\"email\" String \"lastUpdate\" Date}
   :distinct? true/false (only applicable if projections is specified
  "
  [{:keys [kind filter sort-by keys-only projections distinct]}]
  (cond-> (Query. kind)
    filter (.setFilter filter)
    sort-by (add-sorts sort-by)
    keys-only (.setKeysOnly)
    projections (add-projections projections)
    distinct (.setDistinct true)))


;; Fetch options
(defn fetch-options
  "Create fetch options specified by the map. All keys are optional.
    :chunk-size
    :limit
    :offset
    :prefetch-size
    :start-cursor
    :end-cursor
  Prefer :start-cursor over :offset if possible"
  ([] (FetchOptions$Builder/withDefaults))
  ([{:keys [chunk-size start-cursor end-cursor limit offset prefetch-size]}]
   (cond-> (FetchOptions$Builder/withDefaults)
     chunk-size (.chunkSize chunk-size)
     start-cursor (.startCursor (if (string? start-cursor)
                                  (Cursor/fromWebSafeString start-cursor)
                                  start-cursor))
     end-cursor (.endCursor (if (string? end-cursor)
                              (Cursor/fromWebSafeString end-cursor)
                              end-cursor))
     limit (.limit limit)
     offset (.offset offset)
     prefetch-size (.prefetchSize prefetch-size))))

(defn result
  ([ds query-spec]
   (result ds query-spec (fetch-options)))
  ([^DatastoreService ds query-spec fetch-options-spec]
   (.asQueryResultIterable (.prepare ds (query query-spec))
                           (fetch-options fetch-options-spec))))

(defn entity
  ([ds kind filter-or-id]
   (entity ds kind filter-or-id nil))
  ([^DatastoreService ds kind filter-or-id projection]
   (let [filter (cond
                  (integer? filter-or-id) (= key (KeyFactory/createKey kind filter-or-id))
                  (instance? Key filter-or-id) (= key filter-or-id)
                  :else filter-or-id)
         q {:kind kind
            :filter filter
            :projection projection}]
     (.asSingleEntity (.prepare ds (query q))))))

(defn select-properties
  "Returns a map with props => value for an Entity

  Example
  (select-properties e [\"description\" \"name\" \"parentId\"])
  => {\"description\" \"Foo\"
      \"name\" \"Bar\"
      \"parentId\" 0}"
  [^Entity e props]
  (reduce (fn [result prop]
            (assoc result prop (.getProperty e prop)))
          {}
          props))

(comment
  ;; Examples

  (query/result ds {:kind "User"})

  (query/result ds {:kind "User"
                    :filter (query/< "age" 16)})

  (query/result ds {:kind "User"
                    :sort-by "lastName"})

  (query/result ds {:kind "User"
                    :sort-by ["lastName" :desc]})

  (query/result ds {:kind "User"
                    :sort-by [["lastName" :asc]
                              ["age" :desc]]
                    :keys-only true})

  (query/entity ds "User" id-or-filter)
  (query/entity ds "User" id-or-filter projections)



  (require '[akvo.commons.config :as config])
  (require '[akvo.commons.gae :as gae])
  (config/set-settings! "../akvo-flow-services/config/my-config.edn")
  (def ds-spec {:hostname "akvoflowsandbox.appspot.com"
                :port 443
                :service-account-id "account-1@akvoflowsandbox.iam.gserviceaccount.com"
                :private-key-file "../akvo-flow-server-config/akvoflowsandbox/akvoflowsandbox.p12"})

  (gae/with-datastore [ds ds-spec]
    (seq (result ds
                 {:kind "User"
                  :sort-by "emailAddress"
                  ;;:filter (= "permissionList" "10")
                  }
                 {:limit 3
                  :offset 3})))

  )
