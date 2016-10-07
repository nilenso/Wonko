(ns wonko.export.prometheus.create
  (:require [clojure.string :as s]
            [wonko.constants :as c])
  (:import [io.prometheus.client Gauge Counter Summary Histogram]
           [io.prometheus.client CollectorRegistry]))

(defn registry []
  (CollectorRegistry.))

(defn clear-registry [registry]
  (.clear registry))

(defn ->prometheus-name [metric-name]
  (-> metric-name
      name
      (s/replace #"[-/ ]" "_")))

(defn maybe-set-label-names [metric label-names]
  (if (seq label-names)
    (.labelNames metric (into-array (mapv ->prometheus-name label-names)))
    metric))

(defn set-basics [metric metric-name help label-names]
  (-> metric
      (.name (->prometheus-name metric-name))
      (maybe-set-label-names label-names)
      (.help help)))

(defn counter [registry metric-name help label-names]
  (-> (Counter/build)
      (set-basics metric-name help label-names)
      (.register registry)))

(defn gauge [registry metric-name help label-names]
  (-> (Gauge/build)
      (set-basics metric-name help label-names)
      (.register registry)))

(defn summary [registry metric-name help label-names]
  (-> (Summary/build)
      (set-basics metric-name help label-names)
      (.quantile 0.5 0.05)
      (.quantile 0.9 0.01)
      (.quantile 0.99 0.001)
      (.register registry)))

(defn histogram [registry metric-name help label-names
                 {:keys [start width count] :or {start 0} :as bucket-config}]
  (-> (Histogram/build)
      (set-basics metric-name help label-names)
      (.linearBuckets (double start) (double width) (int count))
      (.register registry)))

(defn stream [registry metric-name help-text label-names]
  (let [h-metric-name (str metric-name "-" c/histogram)
        s-metric-name (str metric-name "-" c/summary)]
    {c/histogram (histogram registry h-metric-name help-text label-names
                            {:width 1 :count 30})
     c/summary (summary registry s-metric-name help-text label-names)}))

(defn metric [registry {:keys [metric-name metric-type label-names] :as event}]
  (condp = metric-type
    c/counter (counter registry metric-name "help-text" label-names)
    c/gauge (gauge registry metric-name "help-text" label-names)
    c/stream (stream registry metric-name "help-text" label-names)))
