(defproject webjure/tuck "0.4.4"
  :description "Tuck: a simple helper for UI folding"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.9.946"]]
  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-doo "0.1.8"]]
  :profiles {:dev {:dependencies [[prismatic/dommy "1.1.0"]
                                  [cljs-react-test "0.1.4-SNAPSHOT"
                                   :exclusions [cljsjs/react
                                                cljsjs/react-with-addons]]
                                  [reagent "0.7.0" :exclusions [cljsjs/react]]
                                  [cljsjs/react-with-addons "15.6.1-0"]
                                  [org.clojure/core.async "0.4.474"]
                                  [clj-chrome-devtools "20180310"]]}}
  :cljsbuild {:builds [{:id "prod"
                        :source-paths ["src"]
                        :compiler {:optimizations :advanced
                                   :output-to "tuck.js"
                                   :closure-output-charset "US-ASCII"}}
                       {:id "test"
                        :source-paths ["src" "test"]
                        :compiler {:output-to "target/test.js"
                                   :optimizations :whitespace
                                   :pretty-print  true
                                   :closure-output-charset "US-ASCII"}}]})
