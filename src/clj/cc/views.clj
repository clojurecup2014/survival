(ns cc.views
  (:require [hiccup.core :refer [html]]
            [hiccup.page :refer [html5]]))

(defn base-page []
  (html5 [:div "Hello world this is clojure"]))
