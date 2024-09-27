(ns sharchan.cleanup
  (:require [clojure.core.async :as async]
            [clojure.java.io :as io]
            [sharchan.config :refer [config]]
            [sharchan.db :refer [file-removed-toggle]])
  (:import [java.nio.file Files Paths]
           [java.nio.file.attribute BasicFileAttributes]
           [java.time Instant Duration]))

(defn file-age-in-days [file]
  (let [path (.toPath file)
        attr (Files/readAttributes path BasicFileAttributes (into-array java.nio.file.LinkOption []))
        file-time (.toInstant (.lastModifiedTime attr))
        current-time (Instant/now)]
    (-> (Duration/between file-time current-time)
        (.toDays))))

(defn max-age [file-size]
  (let [max-content-length (apply * (:max-content-length config))
        size-ratio (- (/ file-size max-content-length) 1)
        adjusted-days (- (:min-days config) (:max-days config))]
    (+ (:min-days config) (* adjusted-days (Math/pow size-ratio 3)))))

(defn delete-old-files []
  (doseq [file (file-seq (io/file (:storage-dir-name config)))]
    (when (.isFile file)
      (let [file-size (.length file)
            age (file-age-in-days file)
            stored-limit (max-age file-size)]
        ;;(println "maxage:" stored-limit ", age:" age)
        (when (>= age stored-limit)
          (io/delete-file file)
          (file-removed-toggle (.getName file))
          (println (str "Deleting file: " (.getPath file) ", age: " age)))))))

(defn start-periodic-cleanup [interval-ms]
  (let [cleanup-ch (async/chan)]
    (async/go-loop []
                   (delete-old-files)
                   (async/<! (async/timeout interval-ms))
                   (recur))
    cleanup-ch))
