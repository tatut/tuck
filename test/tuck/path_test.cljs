(ns tuck.path-test
  (:require [tuck.core :as t]
            [tuck.path :refer [by-type]]
            [clojure.test :as test :refer-macros [deftest is async]]))

;; This event is applied to the app root
(defrecord UpdateFoo [to]
  t/Event
  (process-event [{to :to} app]
    (assoc app :foo to)))

;; This event is applied to a path inside the app state
(defrecord IncrementDeep []
  t/Event
  (process-event [_ app]
    (+ app 1)))

;; This too
(defrecord DecrementDeep []
  t/Event
  (process-event [_ app]
    (- app 1)))

;; Path by type not specified, default to app root
(defrecord Bing []
  t/Event
  (process-event [_ app]
    (assoc app :bing "I am the machine that goes BING!")))

(deftest path
  (let [v (atom {:foo :initial
                 :this {:is {:the {:value 41}}}})
        e! (t/control-with-paths
            v
            (by-type UpdateFoo nil

                     #{IncrementDeep DecrementDeep} [:this :is :the :value]))]

    ;; UpdateFoo applied to app root
    (e! (->UpdateFoo "updated foo"))
    (is (= "updated foo" (:foo @v)))

    ;; IncrementDeep/DecrementDeep applied to its defined path
    (e! (->IncrementDeep))
    (is (= 42 (get-in @v [:this :is :the :value])))

    (e! (->DecrementDeep))
    (is (= 41 (get-in @v [:this :is :the :value])))

    ;; Bing applied by default to app root
    (e! (->Bing))
    (is (contains? @v :bing))))
