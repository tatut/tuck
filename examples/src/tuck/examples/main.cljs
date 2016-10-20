(ns tuck.examples.main
  (:require [tuck.core :refer [tuck wrap wrap-path send-value! Event process-event
                               action!
                               send-async!]]
            [tuck.debug :as debug]
            [reagent.core :as r]
            [ajax.core :refer [GET]])
  (:require-macros [tuck.intercept :refer [intercept]]))

(defonce app (r/atom {:counter-a 42
                      :counter-b nil
                      :crazy-counter 10
                      :greeting ""
                      :spotify {:search-term ""
                                :results nil}}))

;; Define our event types
(defrecord Increment [])
(defrecord Decrement [])
(defrecord ChangeGreeting [g])
(defrecord Counter [event counter-key])
(defrecord SearchTrack [name])
(defrecord SearchTrackResult [result])

(extend-protocol Event
  Increment
  (process-event [_ state]
    (inc state))

  Decrement
  (process-event [_ state]
    (dec state))
  
  ChangeGreeting
  (process-event [{g :g} app]
    (assoc app :greeting g))

  Counter
  (process-event [{:keys [counter-key event]} app]
    (update app counter-key
            #(process-event event %)))

  SearchTrack
  (process-event [{name :name} app]
    (action!
     (fn [e!]
       (GET (str "https://api.spotify.com/v1/search?type=track&q=" name)
            {:response-format :json
             :handler #(e! (->SearchTrackResult %))})))
    (assoc app :search-term name))

  SearchTrackResult
  (process-event [result app]
    (.log js/console "GOT RESULT when app is : " (pr-str app))
    (assoc app :results (get-in result [:result "tracks" "items"]))))


(defn counter [e! value]
  [:div
   [:button {:on-click #(e! (->Increment))} "+1"]
   [:button {:on-click #(e! (->Decrement))} "-1"]
   [:div value]])

(defn spotify-search [e! {:keys [search-term results]}]
  [:div.spotify
   "Search for track: "
   
   [:input {:on-change (send-value! e! ->SearchTrack)
            :value search-term}]
   (when results
     [:ul.results
      (for [{id "id" name "name" album "album" :as r} results]
        ^{:key id}
        [:li.result name " (" (get album "name") ")"])])])

(defn main [e! {:keys [counter-a counter-b crazy-counter greeting spotify]}]
  [:div
   "Counter A: "
   ;; wrap events in a Counter record
   [counter (wrap e! ->Counter :counter-a) counter-a]
   [:br]

   ;; The following has the same effect but
   ;; wrap-path uses the built-in UpdateAt event type
   "Counter B: "
   [counter (wrap-path e! :counter-b) counter-b]
   [:br]
   
   ;; A crazy counter that has increment/decrement swapped
   "Crazy counter:"
   (let [e! (wrap-path e! :crazy-counter)]
     [counter (intercept
               e!
               (Increment i (do (.log js/console "GOT Increment: " i)
                                (e! (->Decrement))))
               (Decrement d (do (.log js/console "Got Decrement: " d)
                                (e! (->Increment)))))
      crazy-counter])
   
   ;; Send greeting events 
   [:input {:value greeting
            :on-change (send-value! e! ->ChangeGreeting)}]
   [:br]
   
   ;; Trying to send non-Events will fail (assert failure in console)
   [:button {:on-click #(e! :D)} "This won't work"]

   [:br]

   [spotify-search (wrap-path e! :spotify) spotify]
   ])

(defn start []
  (r/render [debug/tuck app main]
            (.getElementById js/document "app")))
