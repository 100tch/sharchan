(ns sharchan.config
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

(defn load-config []
  (with-open [r (io/reader "config.edn")]
    (edn/read-string (slurp r))))

(defn check-config []
  (if (.exists (io/file "config.edn"))
    (load-config)
    (do
      (println "file 'config.edn' not found.")
      (System/exit 1))))

(def config (check-config))
