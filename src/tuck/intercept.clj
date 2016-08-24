(ns tuck.intercept)

(defmacro intercept
  "Return a new send function that wraps the given send function.
  The event types specified in intercept forms are intercepted and
  the their respective forms are run instead of sending the event.
  Events that have no intercept are sent unaltered."
  [e! & intercept-forms]
  (let [event (gensym "EVENT")]
    `(let [e!# ~e!
           interceptors# ~(into {}
                                (map
                                 (fn [[type bind form]]
                                   [type `(fn [event#]
                                            (let [~bind event#]
                                              ~form))]))
                                intercept-forms)]
       (fn [event#]
         (if-let [interceptor# (get interceptors# (type event#))]
           (interceptor# event#)
           (e!# event#))))))

(comment
  ;; Simple example of syntax

  ;; Generic messages for a "table of items" type of component
  (defrecord AddRow [data])
  (defrecord RemoveRow [at-position])
  
  ;; We want to translate the generic types into
  ;; our domain specific order model. (for example we store
  ;; the data in a different format)
  (defrecord AddOrderItem [id data])
  (defrecord RemoveOrderItem [id])

  ;; then in our component
  (defn order-view [e! orders]
    [order (intercept
            e!
            (AddRow {:keys [data]}
                    (e! (->AddOrderItem (orders/next-order-id orders)
                                        data)))
            (RemoveRow {p :at-position}
                       (e! (->RemoveOrderItem (:id (nth orders p))))))
     orders]))
