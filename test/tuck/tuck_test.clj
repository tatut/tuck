(ns tuck.tuck-test
  (:require  [clojure.test :as t :refer [deftest]]
             [clj-chrome-devtools.cljs.test :as cljs-test]))

(deftest run-cljs-tests
  (cljs-test/build-and-test "test"))
