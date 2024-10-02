(ns sharchan.util
  (:require [sharchan.config :refer [config]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.edn :as edn])
  (:import java.nio.ByteBuffer))

;; url shortener logic
(def base-chr "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz")

(defn encode [^"[B" b]
  (if (empty? b)
    ""
    (let [s (StringBuilder.)
          zero-count (count (take-while zero? b))]
      (loop [i (BigInteger. 1 b)]
        (when-not (zero? i)
          (.append s (nth base-chr (mod i 62)))
          (recur (quot i 62))))
      (str (str/join (repeat zero-count "0")) (.reverse s)))))

(defn- char-index [c]
  (if-let [index (str/index-of base-chr c)]
    index
    (throw (ex-info (str "Character " (pr-str c) " is not part of Base62 character set.")
                    {:type ::illegal-character
                     :character c}))))

(defn decode [s]
  (if (empty? s)
    (byte-array 0)
    (let [[zeros rest] (split-with #(= % \0) s)
          rest-bytes (-> (reduce (fn [i c] (+ (* i 62) (char-index c)))
                                 (bigint 0)
                                 rest)
                         (biginteger)
                         (.toByteArray))
          zero-count (count zeros)
          rest-offset (if (= 0 (aget rest-bytes 0)) 1 0)
          rest-count (- (count rest-bytes) rest-offset)]
      (-> (ByteBuffer/allocate (+ zero-count rest-count))
          (.position zero-count)
          (.put rest-bytes rest-offset rest-count)
          (.array)))))

;; some util func
(defn ensure-storage-dir []
  (let [storage-dir (io/file (config :storage-dir-name))]
    (when-not (.exists storage-dir)
      (println "storage directory not found. creation...")
      (.mkdirs storage-dir))))

(defn random-in-range [min max]
  (+ min (rand-int (inc (- max min)))))

(defn count-pics-in-dir [dir]
  (let [files (file-seq (io/file dir))]
    (count (filter #(re-matches #".*\.(jpg|jpeg|png|gif)$" (.getName %)) files))))

(defn is-text-file? [file]
  (try
    (slurp file :encoding "ISO-8859-1")
    true
    (catch java.io.IOException e
      false)))

;; translations
(defn load-translations [lang]
  (let [filepath (str "locales/" lang ".edn")
        resource (io/resource filepath)]
    (if resource
      (edn/read-string (slurp resource))
      (edn/read-string (slurp (io/resource "locales/en.edn"))))))

(defn get-translation [lang page]
  (let [translations (load-translations lang)
        common-translation (:common translations)
        page-translation (get translations page)]
    (if page-translation
      (merge common-translation page-translation)
      (let [fallback-page-translation (get (load-translations "en") page)]
        (merge common-translation fallback-page-translation)))))
