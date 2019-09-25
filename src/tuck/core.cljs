(ns tuck.core
  (:require [reagent.core :as r]
            [clojure.spec.alpha :as s]
            [tuck.effect :as effect]))

(def ^{:private true
       :dynamic true
       :doc "Bound during process-event to the current UI send function."}
  *current-send-function* nil)

(def ^{:dynamic true
       :doc "Bound to false when replaying events to disable side-effects."}
  *allow-actions* true)

(defprotocol Event
  (process-event [this app-state]
    "Process this event for the current app state. Must return new state."))

(defrecord UpdateAt [event key-path]
  Event
  (process-event [_ app]
    (update-in app key-path
               #(process-event event %))))

(deftype EffectReturn [app effects])

(defn fx
  "Creates an effect to be returned from a `process-event` invocation.
  Takes the updated `app` state and one or more effects to be
  applied. An effect may be a function of arity 1 or a map
  describing an effect. The map effect is applied by calling
  `tuck.effect/process-effect` multimethod.

  Effects are run after the app state has been updated to its
  new value.
  "
  [app & effects]
  (EffectReturn. app effects))

(defn send-value!
  "Returns a UI event handler that sends the event's value
  to the UI message processing after calling constructor with it.
  Useful for DOM change effects.

  Example:
  ```clojure
  [:input {:type \"text\"
           :value name
           :on-change (t/send-value! e! ->UpdateName)}]
  ```"
  {:doc/format :markdown}
  [e! constructor & args]
  (fn [e]
    (.stopPropagation e)
    (e! (apply constructor (-> e .-target .-value) args))))

(defn current-send-function
  "Get the current send function. Must be called from within process-event."
  []
  (assert (not (nil? *current-send-function*)) "current-send-function called outside of process-event")
  *current-send-function*)

(defn send-async!
  "Returns a callback function which will create and apply a new event.
  The `constructor` must be a function that returns an event. The constructor is called with the
  callback parameters and the (optional) arguments.

  **Must be called from within [[process-event]].**

  Example:
  ```clojure
  ;; inside some event's processing code
  (GET! \"/fetch-things\" {:on-success (t/send-async! ->FetchThingsResponse)
                         :on-failure (t/send-async! ->ServerError)})
  ```"
  {:doc/format :markdown}
  [constructor & args]
  (assert (not (nil? *current-send-function*)) "send-async! called outside of process-event")
  (let [e! *current-send-function*]
    (fn [& values]
      (e! (apply constructor (concat values args))))))

(defn action!
  "Run an action function that may side-effect and schedule asynchronous actions.
  The first parameter of the action is the current send function. Actions only run
  when *allow-actions* is true."
  [action-fn & args]
  (assert (not (nil? *current-send-function*)) "action! called outside of process-event")
  (when *allow-actions*
    (let [e! *current-send-function*]
      (apply action-fn e! args))))


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

(defn- validate [previous-app-state event new-app-state spec on-invalid-state]
  (if (or (nil? spec)
          (s/valid? spec new-app-state))
    new-app-state
    (on-invalid-state previous-app-state event new-app-state spec)))

(defn- process-effect* [e! effect]
  (if (fn? effect)
    (effect e!)
    (effect/process-effect e! effect)))

(defn- process-event* [e! event current-app-state]
  (let [ret (process-event event current-app-state)]
    (if (instance? EffectReturn ret)
      ;; This is an effect return, set timeout to process effects and return new app state
      (let [new-app-state (.-app ret)]
        (.setTimeout js/window
                     #(doseq [effect (.-effects ret)]
                      (process-effect* e! effect)) 0)
        new-app-state)

      ;; This is the new app state, return it directly
      ret)))

(defn- ui-send [app path-fn spec on-invalid-state event]
  (assert (satisfies? Event event))
  (let [path (path-fn event)]
    (if path
      (swap! app
             (fn [current-app-state]
               (let [new-app-state
                     (update-in current-app-state path
                                (fn [current-app-state-in-path]
                                  (process-event* *current-send-function* event current-app-state-in-path)))]
                 (validate current-app-state event new-app-state
                           spec on-invalid-state))))
      (swap! app
             (fn [current-app-state]
               (let [new-app-state (process-event* *current-send-function* event current-app-state)]
                 (validate current-app-state event new-app-state
                           spec on-invalid-state)))))))

(defn- ui-send-partial [e! app path-fn spec on-invalid-state event-constructor initial-event-args & args]
  (binding [*current-send-function* e!]
    (let [event (apply event-constructor (concat initial-event-args args))]
      (ui-send app path-fn spec on-invalid-state event))))

(defn control
  ([app]
   (control app (constantly nil) nil nil))
  ([app path-fn spec on-invalid-state]
   (fn e! [event & args]
     (binding [*current-send-function* (or *current-send-function* e!)]
       (cond
         ;; Got event, just apply it directly
         (satisfies? Event event)
         (ui-send app path-fn spec on-invalid-state event)

         ;; Got function, return an event handler that constructs an event
         (fn? event)
         (r/partial ui-send-partial *current-send-function* app path-fn spec on-invalid-state event args))))))

(defn- control-with-paths [app path-fn]
  (control app path-fn nil nil))

(defn- default-on-invalid-state [previous-state event new-state spec]
  (.warn js/console
         "Discarding invalid state after event: " (pr-str event) "\n"
         (s/explain-str spec new-state))
  previous-state)

(defn tuck
  "Entrypoint for tuck. Takes in a reagent atom and a root component.
  The root component will be rendered with two parameters: a ui control
  function (for sending events to) and the current state of the app atom.

  The optional options map can have the following keys:
  :path-fn   If path-fn is provided, it is called to return a path (for update-in)
             for the event. If the path-fn returns nil for the event, the event is
             applied to the app root. Path-fn is an alternative to wrapping send
             functions for routing events to different parts of the state atom.
  :spec      If specified, the app state is validate against the spec after each
             event. If the new state is invalid the on-invalid-state handler is
             called to fix it.

  :on-invalid-state
             Handler to call when the app state after an event fails spec validation.
             Must return new (fixed) app state. Takes 4 arguments: the previous state,
             the event that caused the invalid state, the new invalid state and the
             spec it was validated against.
             Default implementation logs the event and clojure.spec explain output
             and returns the previous valid state.

  For backwards compatibility, if options is a function, it is interpreted to mean
  the path-fn.

  The options are evaluated once, when the component is created and changes to
  options don't take effect during the component's lifetime."
  ([app root-component] [tuck app root-component {}])
  ([app root-component options]
   (let [options (if (fn? options)
                    {:path-fn options}
                    options)
         {:keys [path-fn spec on-invalid-state]} options
         e! (control app (or path-fn (constantly nil))
                     spec (or on-invalid-state default-on-invalid-state))]
     (fn [app root-component _]
       [root-component e! @app]))))
