# ![Tuck logo](https://github.com/tatut/tuck/blob/master/tucklogo.png?raw=true) Tuck

[![Clojars Project](https://img.shields.io/clojars/v/webjure/tuck.svg)](https://clojars.org/webjure/tuck)


Tuck is a minimalistic helper library for UI folding in Reagent.
Tuck defines a protocol for events and how they are processed and provides a simple way to send events from UI code to be processed.

Tuck can be used at any level (not just at the app root): simply pass tuck a reagent atom and a component.

Tuck is heavily inspired by [Petrol](https://github.com/krisajenkins/petrol) but is even lighter and has no dependencies (aside form Reagent itself).

## Usage

Clone this repo and run "lein figwheel dev" in the examples folder.

