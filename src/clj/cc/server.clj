(ns cc.server
  (:require [compojure.core :refer [GET PUT POST DELETE ANY routes]]
            [compojure.route :refer [not-found resources]]
            [ring.adapter.jetty :refer [run-jetty]]
            [com.stuartsierra.component :as component]
            [cc.views.index :as index]
            [clojail.core :refer [sandbox]]
            [clojail.testers :refer [secure-tester]]
            [clojure.edn :as edn]
            [ring.middleware.params]
            [ring.middleware.edn :refer [wrap-edn-params]]
            [ring.middleware.session :as session]))

(def sb (sandbox secure-tester))

(defn edn-response [data & [status]]
  {:status (or status 200)
   :body (pr-str data)
   :headers {"Content-Type" "application/edn"}})

(defn my-routes [{:keys [storage]}]
  (routes
   (resources "/")
   (GET "/" [] (index/page))
   (GET "/test" [] {:status 200
                    :body "{:text \"hi\"}"
                    :headers {"Content-Type" "application/edn"}})
   (POST "/test" [code] (edn-response (try
                                        (sb (clojure.edn/read-string code))
                                        (catch SecurityException e
                                          (str
                                           "You're trying to HACK me?? "
                                           "You giant butt!!!"))
                                        (catch Exception e
                                          (str
                                           "Some error occurred...")))))
   (not-found "404")))

(defn my-handler [{storage :storage :as component}]
  (-> (my-routes component)
      (wrap-edn-params)
      (session/wrap-session {:store storage})))

(defrecord Webserver [config storage server]
  component/Lifecycle
  (start [component]
    (let [server (run-jetty (my-handler component) (assoc config :join? false))]
      (assoc component :server server)))
  (stop [component]
    (if (:server component) (.stop (:server component)))
    (assoc component :server nil)))

(defn new-webserver [config]
  (map->Webserver {:config config}))

