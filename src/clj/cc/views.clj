(ns cc.views
  (:require [hiccup.core :refer [html]]
            [hiccup.page :refer [html5]]
            [environ.core :refer [env]]))

(defn base-page []
  (html5
   [:body
    [:div {:id "app"}]
    (condp = (:profile env :dev)
      :dev (html
            [:script {:src "/js/react.js"}]
            [:script {:src "/js/out/goog/base.js"}]
            [:script {:src "/js/out/app.js"}]
            [:script "goog.require('cc.app');"])
      :prod [:script {:src "/js/out/app.js"}])]))
