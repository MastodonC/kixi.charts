(ns kixi.charts.line-chart
  (:require [cljsjs.d3]
            [goog.userAgent :as agent]
            [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]))

(when (not agent/IE)
  (enable-console-print!))

(defn keywords->str
  "Turns all keywords in sequence
  of maps to strings."
  [m]
  (->> m
       (map (fn [[k v]] {k (name v)}))
       (apply merge)))

(defn create-svg
  "Creates SVG in a div with specified id. Sets
  width, height and margin."
  [div width height margin]
  (-> js/d3 (.select div) (.append "svg:svg")
      (.attr #js {:width  (+ width (:left margin) (:right margin))
                  :height (+ height (:top margin) (:bottom margin))})
      (.append "svg:g")
      (.attr #js {:transform (str "translate(" (:left margin) "," (:top margin) ")")})))

(defn draw-chart [cursor opts]
  (let [data (clj->js (:data cursor))
        {:keys [id style]} opts
        {:keys [margin axis-opts]} style
        width  (-> (.getElementById js/document id) .-clientWidth (- (:left margin) (:right margin)))
        height (-> (.getElementById js/document id) .-clientHeight (- (:top margin) (:bottom margin)))
        {:keys [x-field x-orientation y-field y-orientation
                x-axis-title y-axis-title]} (keywords->str axis-opts)
        x       (-> js/d3 .-time (.scale) (.range (to-array [0 width])))
        y       (-> js/d3 .-scale (.linear) (.range (to-array [height 0])))
        x-axis  (-> js/d3 .-svg (.axis) (.scale x) (.orient x-orientation))
        y-axis  (-> js/d3 .-svg (.axis) (.scale y) (.orient y-orientation))
        line    (-> js/d3 .-svg (.line) (.x (fn [d] (x (aget d x-field))))
                    (.y (fn [d] (y (aget d y-field)))))
        svg     (create-svg (str "#" id) width height margin)]

    (-> x (.domain (-> js/d3 (.extent data (fn [d] (aget d x-field))))))
    (-> y (.domain (-> js/d3 (.extent data (fn [d] (aget d y-field))))) )

    (-> svg
        (.append "g")
        (.attr "class" "x axis")
        (.attr "transform" (str "translate(0," height ")"))
        (.call x-axis))

    (-> svg
        (.append "g")
        (.attr "class" "y axis")
        (.call y-axis)
        (.append "text")
        (.attr "transform" "rotate(-90)")
        (.attr "y" 6)
        (.attr "dy" ".71em")
        (.style "text-anchor" "end")
        (.text y-axis-title))

    (-> svg
        (.append "path")
        (.datum data)
        (.attr "class" "line")
        (.attr "d" line))))

(defn simple-line-chart
  "Creates a simple line chart that
  is styled though CSS.

  Div that will contain the chart has to be created in an
  ealier step and its id passed in opts.
  If using Bootstrap, height of that div must be set since
  Bootstrap sets div height to the size of the content, and
  the content has not been rendered at this point. We cannot
  create a chart if the height of the div is 0.

  Cursor must contain the following:
  :data [{:foo \"a\" :bar \"b\"}
         {:foo \"c\" :bar \"d\"}]

  Options should have:

  :id \"chart\"
  :style {:margin {:top 20 :right 20 :bottom 30 :left 50}
          :min-height 300
          :axis-opts {:x-field :foo
                      :x-orientation :bottom
                      :x-axis-title \"Foo\"
                      :y-field :bar
                      :y-orientation :left
                      :y-axis-title \"Indicator Values\"}}



  Values are an example. "
  [cursor owner {:keys [id] :as opts}]
  (reify
    om/IRender
    (render [_]
      (html
       [:div]))
    om/IDidMount
    (did-mount [_]
      (.addEventListener js/window
                         "resize"
                         (fn [] (om/refresh! owner)))
      (let [n    (.getElementById js/document id)]
        (while (.hasChildNodes n)
          (.removeChild n (.-lastChild n)))
        (draw-chart cursor opts)))
    om/IDidUpdate
    (did-update [_ _ _]
      (let [n    (.getElementById js/document id)]
        (while (.hasChildNodes n)
          (.removeChild n (.-lastChild n)))
        (draw-chart cursor opts)))))
