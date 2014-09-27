{:dev {:dependencies [[org.clojure/tools.namespace "0.2.5"]]
       :plugins [[com.cemerick/austin "0.1.5"]]
       :source-paths ["dev"]
       :env {:server {:port 8080}
             :storage {:uri "datomic:dev://localhost:4334/app"}
             :profile :dev}}}
