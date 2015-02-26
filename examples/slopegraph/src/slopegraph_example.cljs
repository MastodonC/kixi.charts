(ns examples.line-chart.line-chart-example
  (:require [om.core                :as om :include-macros true]
            [sablono.core           :as html :refer-macros [html]]
            [kixi.charts.line-chart :as lc]))

(def app-state (atom {:chart {:column-headings ["2013" "Y2014"]
                              :data (zipmap ["Alice" "Bob" "Chuck" "Dave" "Erin" "Frank" "Oscar" "Peggy" "Sybil" "Walter"]
                                            (map vector (shuffle (range 1 11)) (shuffle (range 1 11))))}}))

(defn example-chart [cursor owner]
  (reify
    om/IRender
    (render [_]
      (html
       [:div {:id "chart" :style {:width "1024" :height "4096"}}
        (om/build kc/slopegraph (:chart cursor)
                  {:opts {:id "chart"
                          :style {:margin {:top 50 :left 20 :right 20}}
                          :label-width 300}})]))))

(om/root example-chart
         app-state
         {:target (. js/document (getElementById "app"))})
