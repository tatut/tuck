(ns tuck.chrome-test
  (:require  [clojure.test :refer [deftest]]
             [clj-chrome-devtools.cljs.test :refer [build-and-test]]))

(deftest run-chrome-tests
  (build-and-test "test"
                  '[tuck.core-test
                    tuck.intercept-test
                    tuck.path-test
                    tuck.spec-test]
                  {:screenshot-video? true
                   :framerate 2}))
