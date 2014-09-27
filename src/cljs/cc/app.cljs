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

(def app-state (atom {:text ""}))

(def c (chan))

(go (while true
      (let [e (<! c)]
        (.log js/console e))))

(defn handle-new-code [code-str state owner]
  (edn-xhr {:method :post
            :data {:code code-str}
            :url "/test"
            :channel c}))

(defn code-editor [state owner]
  (reify
    om/IRender
    (render [_]
      (html
       [:div#editor
        [:textarea {:onBlur #(handle-new-code (.-value (.-target %)) state owner)}]]))))

(om/root code-editor app-state {:target (gdom/getElement "app")})

