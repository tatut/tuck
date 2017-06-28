(ns tuck.core-test
  (:require [tuck.core :as sut]
            [clojure.test :refer [use-fixtures deftest is async]]
            [cljs-react-test.simulate :as sim]
            [cljs-react-test.utils :as tu]
            [reagent.core :as r]
            [tuck.core :as t]
            [dommy.core :as dommy]
            [tuck.testutils :refer [container-fixture sel1 sel after c]]))


(use-fixtures :each container-fixture)

(defrecord ChangeValue [to]
  t/Event
  (process-event [_ app]
    (assoc app :value to)))

(defn simple-input-component
  [e! {value :value}]
  [:input#i1 {:value value
              :on-change (t/send-value! e! ->ChangeValue)}])

(deftest simple-input
  (let [app (r/atom {:value ""})]
    (r/render [t/tuck app simple-input-component] @c)

    (sim/change (sel1 :#i1) {:target {:value "foo"}})

    (r/force-update-all)

    (is (= "foo" (:value @app) (.-value (sel1 :#i1))))))

(deftest wrap-path
  (let [app (r/atom {:deeply {:nested [:not-me {:value "ME"} :not-me-either]}})
        component (fn [e! app]
                    [simple-input-component
                     (t/wrap-path e! :deeply :nested 1)
                     (get-in app [:deeply :nested 1])])]

    (is (nil? (sel1 :#i1)))

    (r/render [t/tuck app component] @c)

    (= (.-value (sel1 :#i1)) "ME")

    (sim/change (sel1 :#i1) {:target {:value "WRAP ME!"}})

    (r/force-update-all)

    (is (= "WRAP ME!" (get-in @app [:deeply :nested 1 :value]) (.-value (sel1 :#i1))))
    (is (= @app {:deeply {:nested [:not-me {:value "WRAP ME!"} :not-me-either]}}))))

;;;;;;;;;;;;
;; Fake AJAX call

(defrecord SearchResults []
  t/Event
  (process-event [_ app]
    ;; Provide some fake results
    (assoc app
           :search-in-progress? false
           :results [{:id 1 :url "http://catpics.example.com/cat1.jpg" :name "Cat doing funny things"}
                     {:id 2 :url "http://cats-r-us.example.com/cat2.gif" :name "Dancing cat"}])))

(defrecord SearchTerm [term]
  t/Event
  (process-event [_ app]
    (let [search? (>= (count term) 3)]
      (when search?
        (after 32 (t/send-async! ->SearchResults)))
      (assoc app
             :term term
             :search-in-progress? search?))))



(defn search [e! {:keys [term results search-in-progress?]}]
  [:div.search
   "Search for: " [:input#search
                   {:value term
                    :on-change (t/send-value! e! ->SearchTerm)}]
   (when search-in-progress?
     [:div.loader "Searching..."])
   (when-not (empty? results)
     [:ul.results
      (for [{:keys [id name url]} results]
        ^{:key id}
        [:li.result [:a {:href url} name]])])])

(deftest async-search-test
  (let [app (r/atom {})]
    (async
     done

     (r/render [t/tuck app search] @c)

     ;; after initial render, search term is empty, no results or loader is showing
     (is (= (.-value (sel1 :#search)) ""))
     (is (nil? (sel1 :div.loader)))
     (is (nil? (sel1 :ul)))

     ;; writing 2 characters does not trigger search yet
     (sim/change (sel1 :#search) {:target {:value "ca"}})
     (r/force-update-all)
     (is (= "ca" (.-value (sel1 :#search)) (:term @app)))
     (is (not (:search-in-progress? @app)))


     ;; writing a third character triggers search
     (sim/change (sel1 :#search) {:target {:value "cat"}})
     (r/force-update-all)
     (is (= "cat" (.-value (sel1 :#search)) (:term @app)))
     (is (:search-in-progress? @app))
     (is (sel1 :.loader))

     (after 100
            #(do
               ;; Results have appeared
               (is (nil? (sel1 :.loader)))
               (is (= 2 (count (sel :li.result))))
               (is (= "Dancing cat" (.-innerHTML (sel1 "li.result:nth-child(2) a"))))
               (done))))))

(defrecord AsyncNoArgs []
  t/Event
  (process-event [_ app]
    (assoc app :async-no-args :ok)))

(defrecord CallAsync []
  t/Event
  (process-event [_ app]
    (let [call (t/send-async! ->AsyncNoArgs)]
      (.setTimeout js/window call 10))
    (assoc app :calling-async-no-args true)))

(defn async-test [e! app]
  [:button {:on-click #(e! (->CallAsync))} "call async"])

(deftest async-without-args
  (let [app (r/atom {})])
  (async
   done

   (r/render [t/tuck app async-test])

   (sim/click :button {})

   (.setTimeout
    #(do
       (is (= :ok (:async-no-args @app)) "async call has been done")
       (done)))))
