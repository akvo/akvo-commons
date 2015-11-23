(ns akvo.commons.metrics
  (:require [com.stuartsierra.component :as component]
            [clj-statsd :as s]))

(defrecord StatsdComponent [host port prefix statsd]
  component/Lifecycle
  (start [this]
    (if statsd
      this
      (assoc this :statsd (s/setup host port :prefix prefix))))
  (stop [this]
    (if statsd
      (do
        (send s/sockagt nil)
        (swap! s/cfg nil)
        (assoc this :statsd nil))
      this)))

(defn new-statsd [host port prefix]
  (map->StatsdComponent {:host host
                         :port port
                         :prefix prefix}))