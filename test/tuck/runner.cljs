(ns tuck.runner
  (:require  [doo.runner :refer-macros [doo-tests]]
             [tuck.core-test]))

(doo-tests 'tuck.core-test)

