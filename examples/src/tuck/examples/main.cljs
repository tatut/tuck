(ns tuck.examples.main
  (:require [tuck.core :refer [tuck wrap wrap-path send-value! Event process-event]]
            [reagent.core :as r]))

(defonce app (r/atom {:counter-a 42
                      :counter-b nil
                      :greeting ""}))

;; Define our event types
(defrecord Increment [])
(defrecord ChangeGreeting [g])
(defrecord Counter [event counter-key])

(extend-protocol Event
  Increment
  (process-event [_ state]
    (inc state))
  
  ChangeGreeting
  (process-event [{g :g} app]
    (assoc app :greeting g))

  Counter
  (process-event [{:keys [counter-key event]} app]
    (update app counter-key
            #(process-event event %))))


(defn counter [e! value]
  [:div
   [:button {:on-click #(e! (->Increment))} "Add"]
   [:div value]])

(defn main [e! {:keys [counter-a counter-b greeting]}]
  [:div
   "Counter A: "
   ;; wrap events in a Counter record
   [counter (wrap e! ->Counter :counter-a) counter-a]
   [:br]

   ;; The following has the same effect but
   ;; wrap-path uses the built-in UpdateAt event type
   "Counter B: "
   [counter (wrap-path e! :counter-b) counter-b]

   ;; Send greeting events 
   [:input {:value greeting
            :on-change (send-value! e! ->ChangeGreeting)}]
   [:br]
   
   ;; Trying to send non-Events will fail (assert failure in console)
   [:button {:on-click #(e! :D)} "This won't work"]])

(defn start []
  (r/render [tuck app main]
            (.getElementById js/document "app")))
