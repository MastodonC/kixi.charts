(defproject kixi/charts "0.1.0-SNAPSHOT"
  :description "Charting components built with d3.js and Om."
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2496" :scope "provided"]
                 [org.omcljs/om "0.8.8"]
                 [sablono "0.3.1"]
                 [cljsjs/d3 "3.5.3-0"]]

  :plugins [[lein-cljsbuild "1.0.4"]]

  :cljsbuild {:builds [{:id "test"
                        :source-paths ["src"]
                        :compiler {:output-to     "resources/js/kixi_charts.js"
                                   :output-dir    "resources/js/out"
                                   :optimizations :none
                                   :source-map true
                                   :pretty-print true}}
                       ;; examples
                       {:id "simple-line-chart"
                        :source-paths ["src" "examples/line_chart/src"]
                        :compiler {:output-to "examples/line_chart/example.js"
                                   :output-dir "examples/line_chart/out"
                                   :source-map true
                                   :optimizations :none}}
                       ]})
