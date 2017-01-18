(ns tuck.intercept-test
  (:require [tuck.core :as t]
            [clojure.test :as test :refer-macros [use-fixtures deftest is async]]
            [cljs.core.async :as async :refer [<!]])
  (:require-macros [tuck.intercept :refer [intercept send-to]]
                   [cljs.core.async.macros :refer [go]]))

(defrecord Inc []
  t/Event
  (process-event [_ val]
    (inc val)))

(defrecord Dec []
  t/Event
  (process-event [_ val]
    (dec val)))


(deftest simple-intercept []
  (let [v (atom 41)
        e! (t/control v)]

    ;; Normal event send works
    (e! (->Inc))
    (is (= 42 @v))

    (let [e! (intercept e!
                        (Inc _ (e! (->Dec)))
                        (Dec _ (e! (->Inc))))]

      ;; Intercept swapped events work
      (e! (->Inc))
      (is (= 41 @v))
      (e! (->Dec))
      (is (= 42 @v)))))

(defrecord UpdateFoo [to]
  t/Event
  (process-event [{to :to} app]
    (println "UpdateFoo, to: " to ", app: " app)
    (assoc app :foo to)))

(defrecord UpdateFooDelayed [foo]
  t/Event
  (process-event [{foo :foo} app]
    (println "UpdateFooDelayed")
    (let [send-update-foo-event! (t/send-async! ->UpdateFoo)]
      (go (<! (async/timeout 1))
          (send-update-foo-event! foo)))
    app))

(defrecord ChildEvent [foo]
  t/Event
  (process-event [{foo :foo} app]
    ;; this will never be called, due to intercept
    app))

(deftest intercept-in-wrapped
  (async
   done
   (let [v (atom {:foo :initial-value
                  :bar {:some-child :value}})
         e! (t/control v)]

     ;; Send to child event, which in turn sends to parent
     (let [e! (intercept (t/wrap-path e! :bar)
                         (ChildEvent {foo :foo}
                                     (send-to e! (->UpdateFooDelayed foo))))]
       (e! (->ChildEvent "from child event")))

     (go
       (<! (async/timeout 5))

       (is (= {:foo "from child event"
               :bar {:some-child :value}} @v)
           "Key :foo has updated in the parent")
       (done)))))
