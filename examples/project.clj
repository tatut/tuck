(defproject tuck-examples "0.1.0-SNAPSHOT"
  :description "Tuck: a simple helper for UI folding"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.10.238"]
                 [figwheel "0.5.16"]
                 [reagent "0.8.0-alpha2"]
                 [cljs-ajax "0.7.3"]]
  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-figwheel "0.5.16"]]

  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src" "../src"]
                        :figwheel {:on-jsload "tuck.examples.main/start"}
                        :compiler {:optimizations :none
                                   :source-map true
                                   :output-to "resources/public/js/compiled/tuck.js"
                                   :output-dir "resources/public/js/compiled/out"
                                   :closure-output-charset "US-ASCII"}}

                       ]}

  )
