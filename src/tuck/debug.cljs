(ns tuck.debug
  "Time traveling debugger for Tuck"
  (:require [reagent.core :as r]
            [tuck.core :as tuck])
  (:require-macros [tuck.intercept :refer [intercept]]))

(defrecord SetState [n]) ;; set state to index n (0 for beginning of time)
(defrecord ClientEvent [e]) ;; invoke client event
(defrecord TogglePlay []) ;; toggle playing/pause
(defrecord Play []) ;; play next event

(defn debug-state
  "Create an initial app state for a debugger."
  [client-atom client-component]
  (let [initial-state @client-atom]
    {:states [{:state initial-state}]
     :current-state 0
     :client-component client-component
     :client-state initial-state
     :client-atom client-atom
     :playing? false
     :play-timeout nil}))

(defn- schedule-play! [e!]
  (js/setTimeout #(e! (->Play)) 100))

(extend-protocol tuck/Event
  TogglePlay
  (process-event [_ {:keys [playing? play-timeout] :as debug}]
    (if-not playing?
      (assoc debug
             :playing? true
             :play-timeout (tuck/action! schedule-play!))
      (do
        (when play-timeout
          (js/clearTimeout play-timeout))
        (assoc debug
               :playing? false
               :play-timeout nil))))

  Play
  (process-event [_ {:keys [states current-state client-state] :as debug}]
    (let [last-state (dec (count states))
          last-play? (= current-state (dec last-state))]
      (assoc debug
             :current-state (inc current-state)
             :client-state (:state (nth states (inc current-state)))
             :playing? (not last-play?)
             :play-timeout (when-not last-play?
                             (tuck/action! schedule-play!)))))
  
  SetState
  (process-event [{n :n} {states :states :as debug}]
    (let [n (js/parseInt n)]
      (assoc debug
             :current-state n
             :client-state (:state (nth states n)))))

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
                       :current-state (dec (count (:states d)))
                       :client-state new-state))]
        ;(.log js/console "STATES AFTER: " (pr-str (:states d)))
        d))))

(defn debug-ui [e! {:keys [states current-state client-component client-state]}]
  (let [last-state (dec (count states))]
    [:span
     [client-component
      (intercept e!
                 (:default e
                           (do
                             (.log js/console "CLIENT EVENT: " e)
                             (e! (->ClientEvent e)))))
      client-state]

     [:div.tuck-debugger {:style {:border "solid 1px black"
                                  :position "fixed"
                                  :bottom 0
                                  :left 0
                                  :right 0
                                  :height 100
                                  :background-color "wheat"}}
      ;; PENDING: style this panel and add slider control for time
      "State:" current-state " / " last-state
      [:div.debugger-controls {:style {:display "flex"
                                       :flex-direction "column"
                                       :align-items "center"}}
       [:input.state-slider 
        {:style {:width 500}
         :type "range" :min 0 :max last-state :value current-state
         :on-change (tuck/send-value! e! ->SetState)}]
       
       [:div.state-buttons {:style {:width "100%"
                                    :display "flex"
                                    :flex-direction "row"
                                    :justify-content "center"}}
        [:button.first-state {:disabled (zero? current-state)
                              :on-click #(e! (->SetState 0))} "<<"]
        [:button.previous-state {:disabled (zero? current-state)
                                 :on-click #(e! (->SetState (dec current-state)))} "<"]
        [:button.play {:disabled (= current-state last-state)
                       :on-click #(e! (->TogglePlay))} "\u25b6"]
        [:button.next-state {:disabled (= current-state last-state)
                             :on-click #(e! (->SetState (inc current-state)))} ">"]
        [:button.last-state {:disabled (= current-state last-state)
                             :on-click #(e! (->SetState (dec (count states))))} ">>"]]]
      [:div.state-info
       [:div.event #_(type (:event (nth states current-state))) 
        ]]]]))

(defn tuck [app root-component]
  (let [debugger-state (r/atom (debug-state app root-component))]
    (add-watch debugger-state
               ::debug-log
               (fn [_ _ _ new-state]
                 (.log js/console "DBG => " (pr-str (dissoc new-state :client-component)))))
    (fn [app root-component]
      [tuck/tuck debugger-state debug-ui])))


