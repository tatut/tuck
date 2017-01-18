(ns tuck.path
  "Helpers for providing event -> path functions.")

(defn by-type
  "Returns a function that dispatches to different path by the type of
  the event. The types and events are alternating in the arguments.
  The type may be a single type or a collection of types to easily add
  the same path for multiple event types."
  [& types-and-paths]
  (let [mapping (reduce (fn [mapping [type path]]
                          (if (coll? type)
                            (merge mapping
                                   (zipmap type
                                           (repeat path)))
                            (assoc mapping type path)))
                        {}
                        (partition 2 types-and-paths))]
    (fn [event]
      (mapping (type event)))))
