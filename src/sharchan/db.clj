(ns sharchan.db
    (:require [next.jdbc :as jdbc]
              [clojure.java.io :as io]
              [sharchan.config :refer [config]]
              [sharchan.util :refer [is-text-file?]]
              [digest :as digest]))

;; database setup (using next.jdbc)
(def db-spec {:dbtype "sqlite" :dbname "sharchan.db"})

(defn create-tables []
  (jdbc/execute! db-spec ["CREATE TABLE IF NOT EXISTS urls (id INTEGER PRIMARY KEY AUTOINCREMENT, url VARCHAR)"])
  (jdbc/execute! db-spec ["CREATE TABLE IF NOT EXISTS files (id INTEGER PRIMARY KEY AUTOINCREMENT, sha256 BINARY, ext VARCHAR, mime VARCHAR, removed BOOLEAN DEFAULT FALSE)"])
  (jdbc/execute! db-spec ["CREATE TABLE IF NOT EXISTS metrics (id INTEGER PRIMARY KEY AUTOINCREMENT, name VARCHAR, count INTEGER DEFAULT 0)"]))

(defn file-exists? [filehash]
  (jdbc/execute-one! db-spec ["SELECT * FROM files WHERE sha256 = ?"
                              (byte-array (.getBytes filehash))]))

(defn file-removed? [filehash]
  (jdbc/execute-one! db-spec ["SELECT * FROM files WHERE sha256 = ? AND removed = TRUE"
                              (byte-array (.getBytes filehash))]))

(defn file-removed-toggle [filehash]
  (jdbc/execute-one! db-spec ["UPDATE files SET removed = NOT removed WHERE sha256 = ?"
                              (byte-array (.getBytes filehash))]))

(defn update-file-metadata [sha256-hash ext mime-type]
  (jdbc/execute-one! db-spec ["UPDATE files SET ext = ?, mime = ? WHERE sha256 = ?"
                              ext mime-type (byte-array (.getBytes sha256-hash))]))

(defn get-file-metadata [sha256-hash]
  (jdbc/execute-one! db-spec ["SELECT ext, mime FROM files WHERE sha256 = ?"
                              (byte-array (.getBytes sha256-hash))]))

(defn update-metric [name]
  (let [existing-metric (jdbc/execute-one! db-spec ["SELECT * FROM metrics WHERE name = ?" name])]
    (if existing-metric
      (jdbc/execute! db-spec ["UPDATE metrics SET count = count + 1 WHERE name = ?" name])
      (jdbc/execute! db-spec ["INSERT INTO metrics (name, count) VALUES (?, 1)" name]))))

(defn save-file [params]
  (let [{:keys [tempfile filename content-type size]} (:file params)]
        (if-not (instance? java.io.File tempfile)
          {:status 400 :body "Uploaded file is not a valid file\n"}

          ;; check for exceeding maximum file size
          (if(> size (apply * (:max-content-length config)))
            {:status 413 :body "File size exceeds the maximum allowed size\n"}

            (let [data (slurp tempfile :encoding "ISO-8859-1")
                  sha256-hash (digest/sha-256 data)
                  is-text? (is-text-file? tempfile)
                  mime-type (or content-type
                                (if is-text? "text/plain" "application/octet-stream"))
                  ext (or (second (re-find #"\.([a-zA-Z0-9]+)$" filename))
                                (if is-text? "txt" "bin"))
                  existing-file (file-exists? sha256-hash)]

              (cond
                (and existing-file (= 1 (:files/removed existing-file)))
                ;; in case a file with such a hash already existed and removed
                (do
                    (file-removed-toggle sha256-hash)
                    (update-file-metadata sha256-hash ext mime-type)
                    (io/copy tempfile (io/file (str "storage/" sha256-hash)))
                    {:status 200 :body (str "/f/" sha256-hash "\n")})

                (nil? existing-file)
                ;; creating a new record in the database
                (do
                  (jdbc/execute-one! db-spec ["INSERT INTO files (sha256, ext, mime) VALUES (?, ?, ?)"
                                              (byte-array (.getBytes sha256-hash)) ext mime-type])
                  
                  (io/copy tempfile (io/file (str "storage/" sha256-hash)))
                  {:status 201 :body (str "/f/" sha256-hash "\n")})

                :else
                {:status 409 :body "Conflict: File already exists\n"}))))))

(defn get-url [url]
  (jdbc/execute-one! db-spec ["SELECT * FROM urls WHERE url = ?" url]))

(defn get-url-by-id [id]
  (jdbc/execute-one! db-spec ["SELECT * FROM urls WHERE id = ?" id]))

(defn count-files []
  (let [non-deleted-query ["SELECT COUNT(*) AS total_non_deleted_files FROM files WHERE removed = FALSE"]
        deleted-query ["SELECT COUNT(*) AS total_deleted_files FROM files WHERE removed = TRUE"]
        non-deleted-count (jdbc/execute-one! db-spec non-deleted-query)
        deleted-count (jdbc/execute-one! db-spec deleted-query)]
    {:total-non-deleted-files (:total_non_deleted_files non-deleted-count)
     :total-deleted-files (:total_deleted_files deleted-count)}))

(defn total-api []
  (let [result (jdbc/execute-one! db-spec ["SELECT SUM(count) AS total FROM metrics"])]
    (if (empty? result)
      0
      (:total result))))

(defn create-url [url]
  (let [url-hash (subs url (inc (.lastIndexOf url "/")))
        existing (get-url url-hash)]
    (if existing
      existing
      (do
        (jdbc/execute-one! db-spec ["INSERT INTO urls (url) VALUES (?)" url-hash])
        (get-url url-hash)))))
