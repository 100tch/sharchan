(ns sharchan.handlers
  (:require [sharchan.db :refer [create-url get-url get-url-by-id file-removed? save-file get-file-metadata]]
            [sharchan.util :refer [encode decode]]
            [sharchan.config :refer [config]]
            [clojure.java.io :as io]
            [ring.util.response :refer [file-response response]]))

(defn shorten [url]
  (if (or (nil? url)
          (not (re-matches #"https?://.*" url)))
    {:status 400 :body "URL parameter is invalid or missing\n"}
    (if (> (count url) (:max-url-length config))
      {:status 414 :body "URI Too Long"}
      (let [url-record (create-url url)]
        (if (nil? (:urls/id url-record))
          {:status 500 :body "Error: could not retrieve ID from URL record\n"}
          (response (str "/f/" (encode (.getBytes (str (:urls/id url-record)))) "\n")))))))

(defn handle-file [params]
  (let [response-after (save-file params)]
    response-after))

(defn handle-put-chunked [body tempfile]
  (let [buffer-size 1024]
    (with-open [out-stream (io/output-stream tempfile)]
      (loop [buffer (byte-array buffer-size)
             bytes-read (.read body buffer)]
        (if (neg? bytes-read)
          {:status 201 :body "Chanks uploaded successfully\n"}
          (do
            (.write out-stream buffer 0 bytes-read)
            (recur buffer (.read body buffer))))))))

(defn handle-file-put [body params headers]
  (let [filename (get params "filename" "uploaded_file")
        content-type (get headers "content-type" "application/octet-stream")
        transfer-encoding (get headers "transfer-encoding")
        tempfile (java.io.File/createTempFile "put-upload" ".tmp")]

    (if (= transfer-encoding "chunked")
      (do
        (handle-put-chunked body tempfile)
        (save-file {:file {:tempfile tempfile 
                           :filename filename
                           :content-type content-type
                           :size (.length tempfile)}}))
      (let [content-length (get headers "content-length")]
        (if content-length
          (with-open [out (io/output-stream tempfile)]
            (io/copy (slurp body) out)
            (save-file {:file {:tempfile tempfile
                               :filename filename
                               :content-type content-type
                               :size (Long/parseLong content-length)}}))
          {:status 411 :body "Length Required (Could not determine remote file size (no Content-Length in response header))\n"})))))

(defn build-file-response [file-path file-hash metadata]
  (-> (file-response file-path)
      (assoc :headers {"Content-Type" (:files/mime metadata)
                       "Content-Disposition" (str "attachment; filename=\"" file-hash "." (:files/ext metadata) "\"")})))

(defn handle-hash [x]
    (let [file-path (str "storage/" x)
          metadata (get-file-metadata x)]
      (cond
        (.exists (io/file file-path))
        (build-file-response file-path x metadata)

        (file-removed? x)
        {:status 410 :body "Gone\n"}

        :else
        (let [id (String. (decode x))
              url-record (get-url-by-id id)]
          (if url-record
            (let [url-file-hash (:urls/url url-record)
                  url-file-metadata (get-file-metadata url-file-hash)
                  url-file-path (str "storage/" url-file-hash)]
              (cond
                (file-removed? url-file-hash)
                {:status 410 :body "Gone\n"}

                (.exists (io/file url-file-path))
                (build-file-response url-file-path url-file-hash url-file-metadata)

                :else
                {:status 404 :body "File not found\n"}))
            {:status 404 :body "File not found\n"})))))

(defn robots-handler [req]
  {:status 200
   :headers {"Content-Type" "text/plain"}
   :body (str "User-agent: *\n"
              "Disallow: /static/\n"
              "Sitemap: " (:preferred-url-scheme config) "://" (:domain config) "/sitemap.xml")})

(defn sitemap-handler [req]
  (let [last-mod (java.time.LocalDate/now)
        last-mod-str (.toString last-mod)]
    {:status 200
     :headers {"Content-Type" "application/xml"}
     :body (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap-image/1.1\">\n"
                "  <url>\n"
                "    <loc>" (:preferred-url-scheme config) "://" (:domain config) "/</loc>\n"
                "    <lastmod>" last-mod-str "</lastmod>\n"
                "  </url>\n"
                "</urlset>")}))
