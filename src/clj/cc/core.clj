(ns cc.core
  (:require [com.stuartsierra.component :as component]
            [cc.server]
            [cc.storage]
            [environ.core :refer [env]]))

(defn system []
  (let [{:keys [server storage]} env]
    (component/system-map
     :storage (cc.storage/new-storage storage)
     :server (component/using
              (cc.server/new-webserver server)
              [:storage]))))
