(ns cc.storage
  (:require [com.stuartsierra.component :as component]
            [clojure.core.async :refer [<! chan]]
            [ring.middleware.session.store :refer [SessionStore]]
            [crypto.password.scrypt :as pw]))

(defn create-user [username password]
  #(let [{next-id :next-id :as storage} %]
     (-> (assoc-in storage [:users next-id] {:username username
                                             :password (pw/encrypt password)
                                             :id next-id})
         (assoc-in [:next-id] (inc next-id)))))

(defn create-user! [{storage :storage} username password]
  (swap! storage (create-user username password)))

(defn lookup-by-username [{storage :storage} username]
  (first (filter #(= (:username %) username) (vals (:users @storage)))))

(defn login [storage username password]
  (when-let [user (lookup-by-username storage username)]
    (when (pw/check password (:password user))
      user)))

(def cs (map char (concat (range 48 58) (range 66 92) (range 97 123))))
(defn rand-chars []
  (cons (rand-nth cs) (lazy-seq (rand-chars))))
(defn rand-str [l] (apply str (take l (rand-chars))))

(def blank-mem-db {:next-id 0
                   :sessions {}
                   :users {}
                   :bots {}})

(defn persist-db! [{{:keys [path to-disk?]} :config storage :storage}]
  (when to-disk?
    (spit path (pr-str @storage))))

(defn read-db [{{:keys [path]} :config storage :storage}]
  (read-string (slurp path)))

(defn new-db [{{:keys [path to-disk?]} :config storage :storage :as component}]
  (if to-disk?
    (try
      (read-db component)
      (catch java.io.IOException _
        (printf "Database %s not found, using test data" path)
        blank-mem-db))
    blank-mem-db))

(defrecord Storage [config storage]
  component/Lifecycle
  (start [component]
    (let [db (atom (new-db component))]
      (assoc component :storage db)))
  (stop [component]
    (let [storage nil]
      (persist-db! component)
      (assoc component :storage nil)))

  SessionStore
  (read-session [{storage :storage} key]
    (get-in @storage [:sessions key]))
  (write-session [{storage :storage} key data]
    (let [key (or key (rand-str 32))]
      (swap! storage assoc-in [:sessions key] data)
      key))
  (delete-session [{storage :storage} key]
    (swap! storage dissoc :sessions key)))

(defn new-storage [config]
  (map->Storage {:config config}))
