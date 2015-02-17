(ns kixi.charts.line-chart
  (:require [cljsjs.d3]
            [goog.userAgent :as agent]
            [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]))

(when (not agent/IE)
  (enable-console-print!))

(defn container-size
  ""
  [id]
  (let [e (.getElementById js/document id)
        x (.-clientWidth e)
        y (.-clientHeight e)]
    {:width x :height y}))

(defn get-size [size id]
  (if-not (nil? (:width size))
    size
    (container-size id)))

(defn keywords->str [m]
  (->> m
       (map (fn [[k v]] {k (name v)}))
       (apply merge)))

(defn create-svg [div width height margin]
  (-> js/d3 (.select div) (.append "svg:svg")
      (.attr #js {:width  (+ width (:left margin) (:right margin))
                  :height (+ height (:top margin) (:bottom margin))})
      (.append "svg:g")
      (.attr #js {:transform (str "translate(" (:left margin) "," (:top margin) ")")})))

(defn draw-chart [{:keys [data size]} opts]
  (let [data (clj->js data)
        {:keys [id style]} opts
        {:keys [width height]} (get-size size id)
        {:keys [margin axis-opts]} style
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
  "Cursor must contain the following:
  :data []

  Options should have:

  :id \"chart\"
  :style {:margin {:top 20 :right 20 :bottom 30 :left 50}}
  :axis-opts {:x-field :year
              :x-orientation :bottom
              :x-axis-title \"Year\"
              :y-field :value
              :y-orientation :left
              :y-axis-title \"Indicator Values\" }

  Values are an example. "
  [cursor owner {:keys [id] :as opts}]
  (reify
    om/IWillMount
    (will-mount [_]
      (.addEventListener js/window
                         "resize"
                         (fn []
                           (om/update! cursor :size (container-size id)))))
    om/IRender
    (render [_]
      (html
       [:div {:id id :style {:height "100%" :width "90%"}}]))
    om/IDidMount
    (did-mount [_]
      (let [n (.getElementById js/document id)]
        (while (.hasChildNodes n)
          (.removeChild n (.-lastChild n)))
        (draw-chart {:data (:data cursor)
                     :size (:size cursor)} opts)))
    om/IDidUpdate
    (did-update [_ _ _]
      (let [n (.getElementById js/document id)]
        (while (.hasChildNodes n)
          (.removeChild n (.-lastChild n)))
        (draw-chart {:data (:data cursor)
                     :size (:size cursor)} opts)))))
