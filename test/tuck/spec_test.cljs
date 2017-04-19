(ns tuck.spec-test
  (:require [tuck.core :as t]
            [clojure.spec :as s]
            [reagent.core :as r]
            [dommy.core :as dommy]
            [cljs-react-test.simulate :as sim]
            [cljs-react-test.utils :as tu]
            [clojure.test :as test :refer-macros [deftest is async]]
            [tuck.testutils :refer [container-fixture sel1 sel after c]]))

(test/use-fixtures :each container-fixture)

(s/def ::app-data (s/keys :req [::name ::age]))
(s/def ::name (s/and string? seq)) ;; non-empty string
(s/def ::age (s/int-in 0 140))

(defrecord SetName [to]
  t/Event
  (process-event [{to :to} app]
    (assoc app ::name to)))

(defrecord SetAge [to]
  t/Event
  (process-event [{to :to} app]
    (assoc app ::age to)))

(defrecord ChangeAge [by]
  t/Event
  (process-event [{by :by} app]
    (update app ::age + by)))

(defn form
  [e! {::keys [name age]}]
  [:div.form
   [:input#name {:value name
                 :on-change (t/send-value! e! ->SetName)}]
   [:input#age {:value age
                :on-change (t/send-value! e! ->SetAge)}]
   [:button#inc {:on-click #(e! (->ChangeAge 1))} "increment age"]
   [:button#dec {:on-click #(e! (->ChangeAge -1))} "decrement age"]])

(deftest cant-make-state-invalid
  (let [app (r/atom {::name "Foo" ::age 1})]
    (r/render [t/tuck app form {:spec ::app-data}] @c)

    ;; Try to change to empty (not allowed by spec
    (sim/change (sel1 :#name) {:target {:value ""}})
    (r/force-update-all)
    (is (= "Foo" (::name @app) (.-value (sel1 :#name))))

    ;; Normal valid change takes place
    (sim/change (sel1 :#name) {:target {:value "Bar"}})
    (r/force-update-all)
    (is (= "Bar" (::name @app) (.-value (sel1 :#name))))

    ;; Click decrement age twice
    (sim/click (sel1 :#dec) {})
    (r/force-update-all)
    (sim/click (sel1 :#dec) {})
    (r/force-update-all)

    (is (= 0 (::age @app) (js/parseInt (.-value (sel1 :#age)))))

    ))
