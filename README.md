# ![Tuck logo](https://github.com/tatut/tuck/blob/master/tucklogo.png?raw=true) Tuck

[![Clojars Project](https://img.shields.io/clojars/v/webjure/tuck.svg)](https://clojars.org/webjure/tuck)
[![CircleCI](https://circleci.com/gh/tatut/tuck.svg?style=svg)](https://circleci.com/gh/tatut/tuck)

Tuck helps you fold away those reset!s and swap!s from your UI views.

Tuck is a minimalistic helper library for UI folding in Reagent.
Tuck defines a protocol for events and how they are processed and provides a simple way to send events from UI code to be processed.

Tuck can be used at any level (not just at the app root): simply pass tuck a reagent atom and a component.

Tuck is heavily inspired by [Petrol](https://github.com/krisajenkins/petrol) but is even lighter and has no dependencies (aside form Reagent itself).

## Documentation

<img src="https://raw.github.com/tatut/tuck/master/docs/tuck-concepts.svg?sanitize=true">

The entrypoint to tuck is the `tuck.core/tuck` reagent component which takes your app state (a ratom) and your 
root component as arguments. Tuck will create a control handle and call your root component with the it and the current 
dereferenced value of the app state. The control handle is used to dispatch events for processing and is typically 
called `e!`. All changes to app state are done by event processing and must be sent through the control handle.

### Minimal example 
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

Events are anything that implement the `tuck.core/Event` protocol. The protocol defines a single method called
`process-event` that takes the event and the current value of the app state and produces a new app state. 

You can define your events as records and define the implementation separately with `extend-protocol` or use the
convenience macro `define-event` which defines the event record and the processing code. The `define-event` macro
has an option called `:path` which is a vector defining a path in the app state where the update should take place.
If no path is defined, the root app state is passed.

Sometimes events just need to assoc a value somewhere in the app state, there is a further convenience macro called
`define-assoc-events` which takes alternating names and app state paths.

## Changes

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
