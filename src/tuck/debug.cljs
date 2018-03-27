(ns tuck.debug
  "Time traveling debugger for Tuck"
  (:require [reagent.core :as r]
            [tuck.core :as tuck :refer-macros [define-event]]
            [cljs.reader :refer [read-string]]
            [clojure.string :as str])
  (:require-macros [tuck.intercept :refer [intercept]]))

(def ^:dynamic *debugger-state-update* false)

(defrecord SetState [n]) ;; set state to index n (0 for beginning of time)
(defrecord ClientEvent [e]) ;; invoke client event
(defrecord TogglePlay []) ;; toggle playing/pause
(defrecord Play []) ;; play next event
(defrecord OutsideStateUpdate [new-state])

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
        d)))

  OutsideStateUpdate
  (process-event [{new-state :new-state} app]
    new-state))

(define-event SetNewWatchPath [path]
  {:path [:new-watch-path]} path)

(define-event AddWatch []
  {}
  (-> app
      (update :watches (fnil conj []) (read-string (:new-watch-path app)))
      (assoc :new-watch-path "")))

(defn- short-fq-name [name]
  (str/replace name #"\w+(\.|/)" (fn [[part]]
                                   (str (subs part 0 1) "."))))


(defn state-replay-controls [e! {:keys [states current-state client-component client-state]}]
  (let [last-state (dec (count states))]
    [:div.tuck-debugger-replay
     "State:" current-state " / " last-state
     (when-let [at-event (and (seq states)
                              (:event (nth states current-state)))]
       [:div (-> at-event type pr-str short-fq-name)])
     [:input.state-slider
      {:style {:width "250px"}
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
                           :on-click #(e! (->SetState (dec (count states))))} ">>"]]]))

(defn- safe-get [obj path]
  (try
    (get-in obj path)
    (catch js/Error e
      {::error e})))

(defn watch-controls [e! {:keys [watches new-watch-path states current-state]}]
  [:div.tuck-debugger-watches
   "Watches:"
   [:table {:style {:width "290px"}}
    [:thead
     [:tr
      [:th {:style {:width 100}} "Path"]
      [:th {:style {:width 180}} "Value"]
      [:th {:style {:width 10}} " "]]]
    (if (seq watches)
      (let [current-states (:state (nth states current-state))
            previous-states (when (pos? current-state)
                              (:state (nth states (dec current-state))))]
        [:tbody
         (doall
          (for [path watches
                :let [path-str (pr-str path)
                      current-val (safe-get current-states path)
                      previous-val (when previous-states
                                     (safe-get previous-states path))
                      changed? (and previous-states (not= current-val previous-val))]]
            ^{:key path-str}
            [:tr {:style (when changed? {:font-weight "bold"})}
             [:td path-str]
             [:td (if-let [err (and (map? current-val)
                                    (::error current-val))]
                    [:div {:style {:display "inline-block"
                                   :color "red"}}
                     err]
                    (pr-str current-val))]
             [:td (when changed? "*")]]))])
      [:tbody
       [:tr
        [:td {:colSpan 3}
         "No watches added"]]])
    [:tbody
     [:tr
      [:td {:colSpan 3}
       [:input {:value new-watch-path
                :placeholder "Add watch path"
                :on-key-down #(when (= 13 (.-keyCode %))
                                 (e! (->AddWatch)))
                :on-change (tuck/send-value! e! ->SetNewWatchPath)}]]]]]])

;; Keep track of whether events are allowed.
;; Some components send events when they are created
;; to initialize stuff. Disallow events when rendering
;; a historical state.
(def allow-events (atom true))

(defn debug-ui [e! {:keys [states current-state client-component client-state] :as debug}]
  (when (not= current-state (dec (count states)))
    ;; NOT AT LATEST STATE, don't allow events during render
    (reset! allow-events false)
    (r/after-render #(reset! allow-events true)))

  [:span
   [client-component
    (tuck/wrap
     (fn [event]
       (when @allow-events
         (e! event)))
     ->ClientEvent)
    client-state]

   [:div.tuck-debugger {:style {:padding "0.3em"
                                :border "solid 1px black"
                                :position "fixed"
                                :bottom 0
                                :right 0
                                :height "100%"
                                :width "300px"
                                :background-color "wheat"
                                :z-index 10000}}


    [:div.debugger-controls {:style {:display "flex"
                                     :flex-direction "column"
                                     :align-items "center"}}
     [state-replay-controls e! debug]
     [watch-controls e! debug]]]])

(defn- outside-state-update [debugger-state]
  (fn [_ _ _ new-state]
    ;; Client state updated
    (when-not *debugger-state-update*
      (.log js/console "Capturing client state updated from outside tuck 😱")
      (swap! debugger-state (fn [debugger-state]
                              (tuck/process-event (->ClientEvent (->OutsideStateUpdate new-state)) debugger-state))))))

(defn- debugger-state-update [app-atom]
  (fn [_ _ _ new-state]
    (binding [*debugger-state-update* true]
      (reset! app-atom (:client-state new-state)))))

(defn tuck [app root-component]
  (r/with-let [debugger-state (r/atom (debug-state app root-component))
               _ (add-watch app ::outside-state-update (outside-state-update debugger-state))
               _ (add-watch debugger-state ::debug (debugger-state-update app))]

    [tuck/tuck debugger-state debug-ui]
    (finally
      (remove-watch app ::outside-state-update)
      (remove-watch debugger-state ::debug))))
