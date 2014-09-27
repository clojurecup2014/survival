(ns cc.server
  (:require [com.stuartsierra.component :as component]))

(defrecord Webserver [config storage server]
  component/Lifecycle
  (start [component]
    (let [server :server]
      (assoc component :server server)))
  (stop [component]
    (let [server nil]
      (assoc component :server nil))))

(defn new-webserver [config]
  (map->Webserver {:config config}))
