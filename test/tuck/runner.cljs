(ns tuck.runner
  (:require  [doo.runner :refer-macros [doo-tests]]
             [tuck.core-test]
             [tuck.intercept-test]
             [tuck.path-test]
             [tuck.spec-test]))

(doo-tests 'tuck.core-test
           'tuck.intercept-test
           'tuck.path-test
           'tuck.spec-test)
