(ns kixi.charts.slopegraph
  (:require [cljsjs.d3]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [sablono.core :as html :refer-macros [html]]))

(defn keywords->str
  "Turns all keywords in sequence
  of maps to strings."
  [m]
  (->> m
       (map (fn [[k v]] {(name k) v}))
       (apply merge)))

(defn create-svg
  "Creates SVG in a div with specified id. Sets
  width, height and margin."
  [div width height margin]
  (-> js/d3 (.select div) (.append "svg:svg")
      (.attr #js {:width  (+ width (:left margin) (:right margin))
                  :height (+ height (:top margin) (:bottom margin))})))


(defn draw-chart [cursor opts]
  ;; TODO use scale,domain,extent from d3 for the y calc.  It's not
  ;; 100% straightforward because of the need to handle ties, where we
  ;; offset the labels into the 'gap' created by the missing
  ;; rank.
  ;; Also the extents for each 'column' could be different again because
  ;; of ties.
  (let [data                       (keywords->str (:data cursor))
        [left-column-heading
         right-column-heading]     (:column-headings cursor)
        {:keys [id style
                label-width]}      opts
        {:keys [margin axis-opts]} style
        width                      (-> (.getElementById js/document id) .-clientWidth (- (:left margin) (:right margin)))
        height                     (-> (.getElementById js/document id) .-clientHeight (- (:top margin) (:bottom margin)))
        [min-rank
         max-rank]                 (reduce (fn [[l1 r1] [l2 r2]] (vector (min l1 r1 l2 r2) (max l1 r1 l2 r2))) (vals data))
        rank-label-psize           12   ; TODO calculate this somehow, query CSS maybe?
        dy                         50
        ypadfactor                 (/ rank-label-psize  (/ height (- max-rank min-rank) ))
        label-offsets              (atom {}) ; We track whether we've
                                             ; seen a rank and offset
                                             ; the next occurrence to
                                             ; avoid overlaps.
        ->y                        (fn [rank] (* (- rank min-rank) (/ height (- max-rank min-rank))))
        ->text-y                   (fn [side rank]
                                     (let [offset (get-in (swap! label-offsets update-in [side rank] (fnil inc -1)) [side rank] 0)]
                                       (->y (+ rank offset)) ))
        svg                        (create-svg (str "#" id) width height margin)
        dxl                        label-width
        dxr                        (- width label-width)]
    (-> svg
        (.append "line")
        (.attr "class" "column-line")
        (.attr "y1" dy)
        (.attr "y2" dy)
        (.attr "x1" 0)
        (.attr "x2" width))

    (-> svg
        (.append "line")
        (.attr "class" "column-line")
        (.attr "y1" 0)
        (.attr "y2" height)
        (.attr "x1" dxl)
        (.attr "x2" dxl))

    (-> svg
        (.append "text")
        (.attr "class" "column-heading-text")
        (.attr "y" dy)
        (.attr "x" 0)
        (.attr "text-anchor" "start")
        (.text left-column-heading))

    (-> svg
        (.append "line")
        (.attr "class" "column-line")
        (.attr "y1" 0)
        (.attr "y2" height)
        (.attr "x1" dxr)
        (.attr "x2" dxr))

    (-> svg
        (.append "text")
        (.attr "class" "column-heading-text")
        (.attr "y" dy)
        (.attr "x" width)
        (.attr "text-anchor" "end")
        (.text right-column-heading))

    (doseq [[label [l r]] data]
      (let [g   (-> svg
                    (.append "g")
                    (.attr "class" "rank-chain"))
            ly  (+ (->y l) dy)
            tly (+ (->text-y :left l) dy)
            ry  (+ (->y r) dy)
            try (+ (->text-y :right r) dy)]
        (when l
          (-> g
              (.append "text")
              (.attr "class" "rank-number")
              (.attr "x" (- dxl 10))
              (.attr "y" ly)
              (.style "text-anchor" "end")
              (.text l))
          (-> g
              (.append "text")
              (.attr "class" "rank-label")
              (.attr "id" (str "l-" label))
              (.attr "x" 0)
              (.attr "y" tly)
              (.style "text-anchor" "start")
              (.text label))
          (-> g
              (.append "circle")
              (.attr "class" "rank-circle")
              (.attr "cx" label-width)
              (.attr "cy" ly)
              (.attr "r" 3)))
        (when r
          (-> g
              (.append "text")
              (.attr "class" "rank-number")
              (.attr "x" (+ 10 dxr))
              (.attr "y" ry)
              (.style "text-anchor" "start")
              (.text r))
          (-> g
              (.append "text")
              (.attr "class" "rank-label")
              (.attr "id" (str "l-" label))
              (.attr "x" width)
              (.attr "y" try)
              (.style "text-anchor" "end")
              (.text label))
          (-> g
              (.append "circle")
              (.attr "cx" dxr)
              (.attr "cy" ry)
              (.attr "r" 3)))
        (when (and l r)
          (-> g
              (.append "line")
              (.attr "y1" ly)
              (.attr "y2" ry)
              (.attr "x1" dxl)
              (.attr "x2" dxr)))))))


(defn slopegraph
  "Creates a slopegraph  chart that
  is styled though CSS.

  Div that will contain the chart has to be created in an
  ealier step and its id passed in opts.
  If using Bootstrap, height of that div must be set since
  Bootstrap sets div height to the size of the content, and
  the content has not been rendered at this point. We cannot
  create a chart if the height of the div is 0.

  Cursor must contain the following:
  :data {:uni_of_bham   [2 1]
         :cambridge_uni [1 2]
         :umist         [3 3]}
  :column-headings [\"2013\" \"2014\"]

  Options should have:

  :id \"chart\"
  :style {:margin {:top 20 :right 20 :bottom 30 :left 50}
          }}

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
