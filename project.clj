(defproject webjure/tuck "0.3"
  :description "Tuck: a simple helper for UI folding"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.225"]]
  :plugins [[lein-cljsbuild "1.1.2"]
            [lein-doo "0.1.7"]]
  :profiles {:dev {:dependencies [[prismatic/dommy "1.1.0"]
                                  [cljs-react-test "0.1.4-SNAPSHOT"]
                                  [reagent "0.6.0-rc"]
                                  [org.clojure/core.async "0.2.395"]]}}
  :cljsbuild {:builds [{:id "prod"
                        :source-paths ["src"]
                        :compiler {:optimizations :advanced
                                   :output-to "tuck.js"
                                   :closure-output-charset "US-ASCII"}}
                       {:id "test"
                        :source-paths ["src" "test"]
                        :compiler {:output-to "target/cljs/test/test.js"
                                   :output-dir "target/cljs/test"
                                   :optimizations :none
                                   :pretty-print  true
                                   :source-map    true
                                   :closure-output-charset "US-ASCII"
                                   :main tuck.runner}}]})
