(ns cc.app
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <! >!]]
            [goog.events :as events]
            [goog.dom :as gdom]
            [cljs.reader :as reader]
            [sablono.core :as html :refer-macros [html]])
  (:import [goog.net XhrIo]
           goog.net.EventType
           [goog.events EventType]))

(def meths
  {:get "GET"
   :put "PUT"
   :post "POST"
   :delete "DELETE"})

(defn edn-xhr [{:keys [method url data channel]}]
  (let [xhr (XhrIo.)
        callback (fn [d] (go (>! channel d)))]
    (events/listen xhr goog.net.EventType.COMPLETE
                   (fn [e] (callback (reader/read-string
                                      (.getResponseText xhr)))))
    (. xhr
       (send url (meths method) (when data (pr-str data))
             #js {"Content-Type" "application/edn"
                  "Accept" "application/edn"}))))

(def c (chan))

(go (while true
      (let [e (<! c)]
        (.log js/console e))))

(defn handle-new-code [code-str state owner]
  (edn-xhr {:method :post
            :data {:code code-str}
            :url "/test"
            :channel c}))

(def error-msg {:failed-auth "Login failed, check your username/password"
                :user-exists "Can't create user, one already exists with that username'"})

(defn change-user-state [state url]
  (let [rc (chan)]
    (edn-xhr {:method :post
              :url url
              :channel rc
              :data {:username  (.-value (gdom/getElement "username"))
                     :password (.-value (gdom/getElement "password"))}})
    (go (let [{:keys [success? error user-id]} (<! rc)]
          (if success?
            (om/transact! state (fn [s]
                                  (-> s
                                      (assoc :logged-in? true)
                                      (assoc :user-id user-id))))
            (om/transact! state (fn [s]
                                  (-> s
                                      (assoc :error (error-msg error))))))))))
(defn login [state] (change-user-state state "/login"))
(defn signup [state] (change-user-state state "/signup"))

(defn app [state owner]
  (reify
    om/IRender
    (render [_]
      (html
       [:div#app
        (when-not (:logged-in? state)
          [:div#login
           [:span#error (:error state)]
           [:input#username {:type "text"}]
           [:input#password {:type "text"}]
           [:button#login {:onClick #(login state)} "Login"]
           [:button#signup {:onClick #(signup state)} "Sign Up"]])
        [:div#editor
         [:textarea
          {:onBlur #(handle-new-code (.-value (.-target %)) state owner)}]]]))))

(defn fetch-initial-state []
  (let [response-chan (chan)]
    (edn-xhr {:method :get :url "/init" :channel response-chan})
    (go (let [initial-state (<! response-chan)]
          (om/root app initial-state {:target (gdom/getElement "app")})))))

(fetch-initial-state)
