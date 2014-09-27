(ns cc.views.index
  (:require [cc.views :refer [base-page]]
            [hiccup.core :refer [html]]))

(defn page []
  (base-page
   (html
    [:div#app])))
