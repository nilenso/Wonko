(ns wonko.utils
  (:import java.util.concurrent.Executors
           java.util.concurrent.ConcurrentHashMap))

(defn create-thread-pool [num-threads]
  (Executors/newFixedThreadPool num-threads))

(defn put-if-absent
  "Calls `.putIfAbsent` on a `ConcurrentHashMap', but returns
   the value whether the put failed or succeeded - the original
   `.putIfAbsent' returns nil if the put succeds; the value otherwise."
  [^ConcurrentHashMap m k v]
  (or (.putIfAbsent m k v) v))
