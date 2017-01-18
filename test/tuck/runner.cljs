(ns tuck.runner
  (:require  [doo.runner :refer-macros [doo-tests]]
             [tuck.core-test]
             [tuck.intercept-test]
             [tuck.path-test]))

(doo-tests 'tuck.core-test
           'tuck.intercept-test
           'tuck.path-test)
