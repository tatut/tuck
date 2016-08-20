(defproject webjure/tuck "0.2"
  :description "Tuck: a simple helper for UI folding"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.89"]
                 [reagent "0.6.0-rc"]]
  :plugins [[lein-cljsbuild "1.1.2"]]

  :cljsbuild {:builds [{:id "prod"
                        :source-paths ["src"]
                        :compiler {:optimizations :advanced
                                   :output-to "tuck.js"
                                   :closure-output-charset "US-ASCII"}}]})


