(ns tuck.core-test
  (:require [tuck.core :as sut]
            [clojure.test :refer [use-fixtures deftest is async]]
            [cljs-react-test.simulate :as sim]
            [cljs-react-test.utils :as tu]
            [reagent.core :as r]
            [tuck.core :as t]
            [dommy.core :as dommy]))

(def ^:dynamic c)

(defn sel1 [selector]
  (dommy/sel1 c selector))
(defn sel [selector]
  (dommy/sel c selector))


(use-fixtures :each
  (fn [test-fn]
    (binding [c (tu/new-container!)]
      (test-fn)
      (tu/unmount! c))))

(defrecord ChangeValue [to]
  t/Event
  (process-event [_ app]
    (assoc app :value to)))

(defn simple-input-component
  [e! {value :value}]
  [:input#i1 {:value value
              :on-change (t/send-value! e! ->ChangeValue)}])

(deftest simple-input
  (let [app (r/atom {:value ""})]
    (r/render [t/tuck app simple-input-component] c)

    (sim/change (sel1 :#i1) {:target {:value "foo"}})

    (r/force-update-all)

    (is (= "foo" (:value @app) (.-value (sel1 :#i1))))))

(deftest wrap-path
  (let [app (r/atom {:deeply {:nested [:not-me {:value "ME"} :not-me-either]}})
        component (fn [e! app]
                    [simple-input-component
                     (t/wrap-path e! :deeply :nested 1)
                     (get-in app [:deeply :nested 1])])]

    (is (nil? (sel1 :#i1)))
    
    (r/render [t/tuck app component] c)

    (= (.-value (sel1 :#i1)) "ME")

    (sim/change (sel1 :#i1) {:target {:value "WRAP ME!"}})

    (r/force-update-all)

    (is (= "WRAP ME!" (get-in @app [:deeply :nested 1 :value]) (.-value (sel1 :#i1))))
    (is (= @app {:deeply {:nested [:not-me {:value "WRAP ME!"} :not-me-either]}}))))
