(ns tuck.core-test
  (:require [tuck.core :as sut]
            [clojure.test :refer [use-fixtures deftest is async]]
            [cljs-react-test.simulate :as sim]
            [cljs-react-test.utils :as tu]
            [reagent.core :as r]
            [tuck.core :as t]
            [dommy.core :refer [sel sel1]]))

(def ^:dynamic c)

(use-fixtures :each
  (fn [test-fn]
    (binding [c (tu/new-container!)]
      (test-fn)
      (tu/unmount! c))))

(defrecord ChangeValue [to]
  t/Event
  (process-event [_ app]
    (assoc app :value to)))

(deftest simple-input
  (let [app (r/atom {:value ""})
        component (fn [e! {value :value}]
                    [:input#i1 {:value value
                                :on-change (t/send-value! e! ->ChangeValue)}])]
    (r/render [t/tuck app component] c)

    (sim/change (sel1 :#i1) {:target {:value "foo"}})

    (r/force-update-all)

    (is (= "foo" (:value @app) (.-value (sel1 :#i1))))))
