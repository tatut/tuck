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
           (conj dates date)))

(defmacro define-event [event-record-name event-params options & body]
  (let [handler-fn-name (gensym (str event-record-name "-handler-"))
        app-sym (or (:app options) 'app)
        event (gensym "event")
        app (gensym "app")]
    `(do
       (defn ~handler-fn-name [~app-sym {:keys ~event-params}]
         ~@body)

       (defrecord ~event-record-name ~event-params
         tuck.core/Event
         (process-event [~event ~app]
           ~(if (:path options)
              `(update-in ~app ~(:path options) ~handler-fn-name ~event)
              `(~handler-fn-name ~app ~event)))))))
