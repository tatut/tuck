# ![Tuck logo](https://github.com/tatut/tuck/blob/master/tucklogo.png?raw=true) Tuck

[![Clojars Project](https://img.shields.io/clojars/v/webjure/tuck.svg)](https://clojars.org/webjure/tuck)
[![CircleCI](https://circleci.com/gh/tatut/tuck.svg?style=svg)](https://circleci.com/gh/tatut/tuck)

Tuck is a micro framework for building Reagent apps that have a clean separation of view code and
event processing code. View components in tuck are just pure functions without any magic.

Tuck defines a protocol for events and how they are processed and provides a simple way to send
events from UI code to be processed.

Tuck can be used at any level (not just at the app root): simply pass tuck a reagent atom and a component.

Tuck is heavily inspired by [Petrol](https://github.com/krisajenkins/petrol) but is even lighter and has no dependencies (aside form Reagent itself).

## Documentation

See the codox generated [API documentation](https://tatut.github.io/tuck/codox/index.html).

<img src="https://raw.github.com/tatut/tuck/master/docs/tuck-concepts.svg?sanitize=true">

The entrypoint to tuck is the `tuck.core/tuck` reagent component which takes your app state (a ratom) and your
root component as arguments. Tuck will create a control handle and call your root component with the it and the current
dereferenced value of the app state. The control handle is used to dispatch events for processing and is typically
called `e!`. All changes to app state are done by event processing and must be sent through the control handle.

### Minimal example

This is a minimal example that shows how to use tuck. Normally you would want to separate your view code and your event
definitions to separate name spaces.

```clojure
(ns tuck-example.core
  (:require [reagent.core :as r]
            [tuck.core :as t :refer-macros [define-event]]))

(define-event UpdateName [new-name]
  {:path [:name]}
  new-name)

(defn my-root-component [e! {:keys [name :as app]]
  [:input {:value name
           :on-change #(e! (->UpdateName (-> % .-target .-value)))}])

(def app-state (r/atom {}))

(defn main []
  (r/render [t/tuck app-state my-root-component]
            (.getElementById js/document "app")))
```

### Defining events

Events are anything that implement the `tuck.core/Event` protocol. The protocol defines a single
method called `process-event` that takes the event and the current value of the app state and
produces a new app state.

You can define your events as records and define the implementation separately with
`extend-protocol` or use the convenience macro `define-event` which defines the event record and the
processing code. The `define-event` macro has an option called `:path` which is a vector defining a
path in the app state where the update should take place. If no path is defined, the root app state
is passed.

Sometimes events just need to assoc a value somewhere in the app state, there is a further
convenience macro called `define-assoc-events` which takes alternating names and app state paths.

### Asynchronous events

**NOTE:** There is a newer (easier to test) way to do side effects, see "Returning effects" below.

An application will most likely need some asynchronous events to communicate with servers or set
timeouts. This can be done by using `tuck.core/send-async!` which must be called within event
processing code. The `send-async!` function takes an event constructor and optional arguments
and returns a callback that will create and and apply the event when called.

You can use `send-async!` to create callbacks to pass to XHR calls.

```clojure
;; Simple async example
(define-event FetchThings
  {}
  ;; Launch an XHR call and set events events as callbacks
  (GET! "/fetch-things" {:on-success (t/send-async! ->FetchThingsResponse)
                         :on-failure (t/send-async! ->ServerError)})
  ;; Return new app state
  (assoc app :fetching-things? true))
```

### Returning effects

A `process-event` implementation may also need to fire off effects like HTTP calls or
other side effects. This can be done by returning an effect descriptor with the `fx` function.

The `fx` function takes the new app state and one (or more) effects that should be run after
the `process-event` is done. An effect may be a function with one argument (the `e!` control handle)
or a map with a `:tuck.effect/type` keyword describing the effect to process.

For function effects, they are simply called and can send further events by calling the `e!` parameter.

For maps, they are run with `tuck.effect/process-effect` multimethod. To create new types of effects,
simply add implementations for that method.

```clojure
;; Same simple event, as in the above send-async! example
(define-event FetchThings
  {}
  (fx
    ;; Return new app state
    (assoc app :fetching-things? true)

    ;; Launch an XHR call by an effect descriptor
    {:tuck.effect/type :http
     :url "/fetch-things"
     :on-success ->FetchThingsResponse}))
```

Tuck does not provide any effect types out of the box. You must provide an implementation of the
`:http` effect type in order to use it.


## Changes

### Version 20181204

* New effect system that is easier to test (see `fx` function and `tuck.effect` namespace)

### Version 20180722

* Changed: improved docstrings (no code changes)

### Version 20180721

* Added: `define-assoc-events` convenience macro

### Version 20180327

* Added: debugger supports watches
* Fixed: debugger state handling improvements

### Version 0.4.4 (2018-03-01)

* Added: new (somewhat) experimental `define-event` macro for defining the record and implementation in one go

### Version 0.4.3 (2017-10-20)

* Performance improvement: Evaluate options once during component creation.

### Version 0.4.2 (2017-10-09)

* Bugfix: Fix binding of *current-send-function*

### Version 0.4.1 (2017-06-28)

Minor fix release

* change clojure.spec namespaces to .alpha suffixed
* allow 0 or more args to send-async! fn (#4)

### Version 0.4 (2017-04-19)

Support clojure.spec validation of app state.
Add options map as 3rd argument that can specify new options:

* :spec
* :on-invalid-state


## Usage

Clone this repo and run "lein figwheel dev" in the examples folder.
