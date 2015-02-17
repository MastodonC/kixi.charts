(ns examples.line-chart.line-chart-example
  (:require [om.core                :as om :include-macros true]
            [sablono.core           :as html :refer-macros [html]]
            [kixi.charts.line-chart :as lc]))

(def app-state (atom {:chart {:data [{:year "2001" :value 3}
                                     {:year "2002" :value 4}
                                     {:year "2003" :value 5}
                                     {:year "2004" :value 1}
                                     {:year "2005" :value 6}]}}))

(defn example-chart [cursor owner]
  (reify
    om/IRender
    (render [_]
      (html
       [:div
        (om/build lc/simple-line-chart (:chart cursor)
                  {:opts {:id "chart"
                          :style {:margin {:top 50 :right 50 :bottom 50 :left 50}
                                  :axis-opts {:x-field :year :x-orientation :bottom
                                              :x-axis-title "Year"
                                              :y-field :value :y-orientation :left
                                              :y-axis-title "Indicator Values"}}}})]))))

(om/root example-chart app-state {:target (. js/document (getElementById "app"))})
