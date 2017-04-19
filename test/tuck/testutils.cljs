(ns tuck.testutils
  (:require [dommy.core :as dommy]
            [cljs-react-test.utils :as tu]))

(def c (atom nil))

(defn sel1 [selector]
  (dommy/sel1 @c selector))

(defn sel [selector]
  (dommy/sel @c selector))

(defn after [millis callback]
  (.setTimeout js/window callback millis))

(def container-fixture
  {:before #(reset! c (tu/new-container!))
   :after #(do (tu/unmount! @c)
               (reset! c nil))})
