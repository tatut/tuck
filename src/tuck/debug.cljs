(ns tuck.debug
  "Time traveling debugger for Tuck"
  (:require [reagent.core :as r]
            [tuck.core :as tuck])
  (:require-macros [tuck.intercept :refer [intercept]]))

(defrecord SetState [n]) ;; set state to index n (0 for beginning of time)
(defrecord ClientEvent [e]) ;; invoke client event

(defn debug-state
  "Create an initial app state for a debugger."
  [client-atom client-component]
  (let [initial-state @client-atom]
    {:states [{:state initial-state}]
     :current-state 0
     :client-component client-component
     :client-state initial-state
     :client-atom client-atom}))

(extend-protocol tuck/Event
  SetState
  (process-event [{n :n} debug]
    (assoc debug :current-state n))

  ClientEvent
  (process-event [{e :e} {:keys [states current-state client-state] :as debug}]
    (let [at-end? (= current-state (count states))
          new-state (tuck/process-event e client-state)]
      (.log js/console (= current-state (dec (count states))))
      (let [d (as-> debug d
                (assoc d :states (conj (if at-end?
                                         states
                                         (subvec states 0 (inc current-state)))
                                       {:state new-state
                                        :event e}))
                (assoc d
                       :current-state (count (:states d))
                       :client-state new-state))]
        ;(.log js/console "STATES AFTER: " (pr-str (:states d)))
        d))))

(defn debug-ui [e! {:keys [states current-state client-component client-state]}]
  [:span
   [client-component
    (intercept e!
               (:default e
                         (do
                           (.log js/console "CLIENT EVENT: " e)
                           (e! (->ClientEvent e)))))
    client-state]

   [:div
    ;; PENDING: style this panel and add slider control for time
    "States:" (count states)]])

(defn tuck [app root-component]
  (.log js/console "COMP!")
  (let [debugger-state (r/atom (debug-state app root-component))]
    (add-watch debugger-state
               ::debug-log
               (fn [_ _ _ new-state]
                 (.log js/console "DBG => " (pr-str (dissoc new-state :client-component)))))
    (fn [app root-component]
      [tuck/tuck debugger-state debug-ui])))


