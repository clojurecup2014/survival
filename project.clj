(defproject cc "1.0.0-SNAPSHOT"
  :description "FIXME: write description"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2342"]
                 [com.stuartsierra/component "0.2.2"]
                 [compojure "1.1.9"]
                 [ring/ring-core "1.3.1"]
                 [ring/ring-jetty-adapter "1.3.1"]
                 [environ "1.0.0"]
                 [hiccup "1.0.5"]
                 [om "0.7.3"]
                 [sablono "0.2.22"]]
  :source-paths ["src/cljs" "src/clj"]
  :cljsbuild {:builds
              [{:id "dev"
                :source-paths ["src/cljs"]
                :compiler {:output-to "resources/public/js/out/app.js"
                           :output-dir "resources/public/js/out"
                           :optimizations :none
                           :pretty-print true
                           :source-map true}}
               {:id "prod"
                :source-paths ["src/cljs"]
                :compiler {:output-to "resources/public/js/out/app.js"
                           :optimizations :advanced
                           :pretty-print false
                           :preamble ["resources/public/js/react.min.js"]
                           :externs ["resources/public/js/react.externs.js"]}}]}
  :plugins [[lein-environ "1.0.0"]
            [lein-cljsbuild "1.0.3"]
            [cider/cider-nrepl "0.8.0-SNAPSHOT"]])
