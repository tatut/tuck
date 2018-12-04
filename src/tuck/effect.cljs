(ns tuck.effect
  "Describe processing effects. Effects may be returned from `process-event`
  method implementations. Effects do not receive current app state, but do receive
  `e!` control handle so that they can send further events.")

(defmulti process-effect
  "Process an effect. Dispatches on `:tuck.effect/type`.
  Do not call this directly, the tuck control handle calls this when
  `process-event` returns an effect.

  To add new effect types, implement this multimethod for a new type keyword."
  (fn [e! effect]
    (::type effect)))

(defmethod process-effect :default
  [e! effect]
  (.warn js/console
         "No tuck.effect/process-effect multmethod implementation for type: " (::type effect)
         "; got effect: " (pr-str effect)))
