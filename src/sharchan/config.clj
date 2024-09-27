(ns sharchan.config
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

(defn load-config []
  (with-open [r (io/reader "config.edn")]
    (edn/read-string (slurp r))))

(def config (load-config))
