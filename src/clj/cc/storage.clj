(ns cc.storage
  (:require [com.stuartsierra.component :as component]))

(defrecord Storage [config storage]
  component/Lifecycle
  (start [component]
    (let [storage :storage]
      (assoc component :storage storage)))
  (stop [component]
    (let [storage nil]
      (assoc component :storage nil))))

(defn new-storage [config]
  (map->Storage {:config config}))
