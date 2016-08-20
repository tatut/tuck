(ns tuck.core
  (:require [reagent.core :as r]))

(def ^{:private true
       :dynamic true
       :doc "Bound during process-event to the current UI send function."}
  *current-send-function* nil)

(defprotocol Event
  (process-event [this app-state]
    "Process this event for the current app state. Must return new state."))

(defrecord UpdateAt [event key-path]
  Event
  (process-event [_ app]
    (update-in app key-path
               #(process-event event %))))

(defn send-value!
  "Returns an event handler that sends the event's value
  to the UI message processing after calling constructor with it."
  [e! constructor & args]
  (fn [e]
    (.stopPropagation e)
    (e! (apply constructor (-> e .-target .-value) args))))

(defn send-async!
  "Returns a callback which sends its argument to the UI after wrapping
  it with the given constructor. Must be called from within process-event."
  [constructor & args]
  (assert (not (nil? *current-send-function*)) "send-async! called outside of process-event")
  (let [e! *current-send-function*]
    (fn [value]
      (e! (apply constructor value args)))))

(defn current-send-function
  "Get the current send function. Must be called from within process-event."
  []
  (assert (not (nil? *current-send-function*)) "current-send-function called outside of process-event")
  *current-send-function*)

(defn wrap
  "Wrap the given UI send function with the given constructor
  and optional arguments. Returns a new UI send function where
  each event is mapped with the constructor before being sent."
  [e! wrap-constructor & args]
  (fn ui-send [event]
    (assert (satisfies? Event event))
    (binding [*current-send-function* (or *current-send-function* ui-send)]
      (e! (apply wrap-constructor event args)))))

(defn wrap-path
  "Wrap the given UI send function with an UpdateAt event for
  the given key-path."
  [e! & key-path]
  (wrap e! ->UpdateAt (vec key-path)))

(defn- control [app]
  (fn ui-send [event]
    (assert (satisfies? Event event))
    (binding [*current-send-function* (or *current-send-function* ui-send)]
      (.log js/console "CURRENT UI SEND " *current-send-function*)
      (swap! app #(process-event event %)))))

(defn tuck
  "Entrypoint for tuck. Takes in a reagent atom and a root component.
  The root component will be rendered with two parameters: a ui control
  function (for sending events to) and the current state of the app atom."
  [app root-component]
  [root-component
   (control app)
   @app])


