(ns tuck.core)

(comment
  (define-event ToggleDate [date]
    {:doc "Docstring"
     :path [:route :dates] ;; route in app state, defaults to root
     :app dates            ;; defaults to "app"
     :pre [(t/after? date (t/now))] ;; normal clojure pre/post conditions
     :post [(not (nil? %))]}
    ;; body, can use all parameters by name and the app state by name
    ;; must return new app state
    (conj dates date))


  ;; Define an event that runs on a worker
  (define-event DoSomeLongCalculation [things]
    {:worker :long-calculation}
    ;; The
    (let [long-result
          (long-function things)]

      ;; send-main! can be used to send a message back to
      ;; main UI
      (tuck/send-main! (->LongCalculationResult long-result))

      ;; the worker has its own app state
      (assoc app :things-being-worked-on )))
  )

(defmacro define-event
  "Define a new event record type and its [[process-event]] implementation.

  - `event-record-name` must be a symbol defining the event name.
  - `event-params` is a vector containing the names of the record's fields.
  - `options` a map of parameters.
  - `body` code to apply the event to current app state.

  Supported options:

  - `:path` a vector containing a path to the app state where the update should take place
  - `:app` a name to bind the current app state to (defaults to `app`)

  The code in `body` can refer to the event params and the app state by their name.
  The app state is the value under `:path` and the returned value will replace it.

  Example:
  ```clojure
  (define-event UpdateCustomerForm [form-data]
    {:path [:customer :editing]
     :app customer}
    (merge customer form-data))
  ```"
  {:doc/format :markdown}
  [event-record-name event-params options & body]
  (let [handler-fn-name (gensym (str event-record-name "-handler-"))
        app-sym (or (:app options) 'app)
        event (gensym "event")
        app (gensym "app")
        worker? (contains? options :worker)
        apply-event
        (if (:path options)
          `(update-in ~app ~(:path options) ~handler-fn-name ~event)
          `(~handler-fn-name ~app ~event))]
    `(do
       (defn ~handler-fn-name [~app-sym {:keys ~event-params}]
         ~@body)

       (defrecord ~event-record-name ~event-params
         tuck.core/Event
         (process-event [~event ~app]
           ~(if worker?
              `(if (not tuck.core/webworker?)
                 ;; This is the main thread
                 (do
                   ;; Send this event to worker
                   (tuck.core/send-worker! ~(:worker options) ~event)
                   ;; and return app state unchanged
                   ~app)

                 ;; This is the worker, apply event
                 ~apply-event)

              ;; Normal event
              apply-event))))))

(defmacro define-assoc-events
  "Define events that simply take their parameter and assoc it to the given path in the app state.
  Takes pairs of event name and path-vector.
  For example:

  `(define-assoc-events UpdateUserName [:user :name])`

  Will define an event record type called UpdateUserName which has one field and
  whose [[process-event]] will update the value of the field to the app state path `[:user :name]`.
  The new value will overwrite the value (if any) in the app state."
  {:doc/format :markdown}
  [& events-and-paths]
  (assert (seq events-and-paths)
          "Events and paths is empty! Specify event name and path in app state.")
  (assert (even? (count events-and-paths))
          "Odd number of arguments. Expected alternating event names and app state paths.")
  `(do
     ~@(for [[name path] (partition 2 events-and-paths)]
         (do
           (assert (symbol? name)
                   (str "Illegal argument. Event name must be a symbol, got: " (pr-str name)))
           (assert (vector? path)
                   (str "Illegal argument. Path must be a vector, got: " (pr-str path)))
           `(define-event ~name [value#]
              {:path ~path}
              value#)))))
