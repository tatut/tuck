(ns tuck.core
  (:require [reagent.core :as r]))

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

(defn wrap
  "Wrap the given UI send function with the given constructor
  and optional arguments. Returns a new UI send function where
  each event is mapped with the constructor before being sent."
  [e! wrap-constructor & args]
  (fn [event]
    (assert (satisfies? Event event))
    (e! (apply wrap-constructor event args))))

(defn wrap-path
  "Wrap the given UI send function with an UpdateAt event for
  the given key-path."
  [e! & key-path]
  (wrap e! ->UpdateAt (vec key-path)))

(defn- control [app]
  (fn [event]
    (assert (satisfies? Event event))
    (swap! app #(process-event event %))))

(defn tuck
  "Entrypoint for tuck. Takes in a reagent atom and a root component.
  The root component will be rendered with two parameters: a ui control
  function (for sending events to) and the current state of the app atom."
  [app root-component]
  [root-component
   (control app)
   @app])


