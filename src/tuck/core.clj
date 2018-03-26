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

(defmacro define-event [event-record-name event-params options & body]
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
