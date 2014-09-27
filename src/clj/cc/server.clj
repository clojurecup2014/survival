(ns cc.server
  (:require [compojure.core :refer [GET PUT POST DELETE ANY routes]]
            [compojure.route :refer [not-found resources]]
            [ring.adapter.jetty :refer [run-jetty]]
            [com.stuartsierra.component :as component]
            [cc.views.index :as index]))

(defn my-routes [{:keys [storage]}]
  (routes
   (resources "/")
   (GET "/" [] (index/page))
   (not-found "404")))

(defrecord Webserver [config storage server]
  component/Lifecycle
  (start [component]
    (let [server (run-jetty (my-routes component) (assoc config :join? false))]
      (assoc component :server server)))
  (stop [component]
    (if (:server component) (.stop (:server component)))
    (assoc component :server nil)))

(defn new-webserver [config]
  (map->Webserver {:config config}))
