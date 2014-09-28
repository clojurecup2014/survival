(ns cc.app
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <! >!]]
            [goog.events :as events]
            [goog.dom :as gdom]
            [cljs.reader :as reader]
            [clojure.string]
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
                :user-exists "Can't create user, one already exists with that username"})

(defn change-user-state [state {:keys [action data]}]
  (let [rc (chan)]
    (edn-xhr {:method :post
              :url (clojure.string/replace-first (str action) #":" "/")
              :channel rc
              :data data})
    (go (let [{:keys [success? error user-id] :as m} (<! rc)]
          (if success?
            (om/transact! state (fn [s]
                                  (-> s
                                      (assoc :user-id user-id)
                                      (dissoc :error))))
            (om/transact! state (fn [s]
                                  (-> s
                                      (assoc :error (error-msg error))))))))))

(defn username [] (.-value (gdom/getElement "username")))
(defn password [] (.-value (gdom/getElement "password")))

(defn login [state] (change-user-state state {:action :login :data {:username (username) :password (password)}}))
(defn signup [state] (change-user-state state {:action :signup :data {:username (username) :password (password)}}))
(defn logout [state] (change-user-state state {:action :logout}))

(defn login-area [login-info owner]
  (reify
    om/IRender
    (render [this]
      (html
       (if-not (:user-id login-info)
         [:div#login
          [:div#error [:span#error (:error login-info)]]
          [:input#username {:type "text"}]
          [:input#password {:type "text"}]
          [:button#login {:onClick #(login login-info)} "Login"]
          [:button#signup {:onClick #(signup login-info)} "Sign Up"]]
         [:button#logout {:onClick #(logout login-info)} "Logout"])))))

(defn user-area [data owner]
  (reify
    om/IRender
    (render [this]
      (html
       [:div#editor
        [:textarea
         {:onBlur #(handle-new-code (.-value (.-target %)) data owner)}]]))))

(defn app [state owner]
  (reify
    om/IRender
    (render [_]
      (html
       [:div#app
        (om/build login-area (:login state))
        (om/build user-area (:data state))]))))

(defn fetch-initial-state []
  (let [response-chan (chan)]
    (edn-xhr {:method :get :url "/init" :channel response-chan})
    (go (let [initial-state (<! response-chan)]
          (om/root app initial-state {:target (gdom/getElement "app")})))))

(fetch-initial-state)
