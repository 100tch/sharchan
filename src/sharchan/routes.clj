(ns sharchan.routes
  (:require [compojure.core :refer [defroutes GET POST PUT]]
            [compojure.route :as route]
            [selmer.parser :refer [render-file]]
            [sharchan.handlers :refer :all]
            [sharchan.db :refer [count-files total-api update-metric]]
            [sharchan.config :refer [config]]
            [sharchan.util :refer [random-in-range]]))

(defn common-params []
  {:file-count (count-files)
   :total-api-requests (total-api)
   :min-age (:min-days config)
   :max-age (:max-days config)
   :max-size (apply * (:max-content-length config))})

(defn links-params []
  {:links (:sharchan-links config)})

;; routes and handlers
(defroutes app-routes
  (GET  "/"            [lang]
       (let [template-path (case lang
                             "ru" "templates/ru/index.html"
                             "templates/index.html")]
         (render-file template-path (merge
                                      {:title-prefix "homepage"}
                                      (common-params)
                                      (links-params)))))

  (GET  "/policy"      [] (render-file "templates/policy.html" (merge
                                                                 {:title-prefix "policy"}
                                                                 (links-params))))

  (GET  "/logo"        [] (render-file "templates/logo.html" (merge
                                                               {:title-prefix "logo"}
                                                               (links-params))))

  (GET  "/robots.txt"  [] robots-handler)

  (GET  "/sitemap.xml" [] sitemap-handler)

  (POST "/shorten"     {params :params}
        (do
          (update-metric "shorter")
          (shorten (get params :url))))

  (POST "/"            {params :params}
        (do
          (update-metric "file-post")
          (handle-file params)))

  (PUT  "/"            {body :body params :params headers :headers}
       (do
         (update-metric "file-put")
         (handle-file-put body params headers)))

  (PUT  "/:filename"   [filename :as {body :body headers :headers}]
       (do
         (update-metric "file-put")
         (handle-file-put body {:filename filename} headers)))

  (GET  "/f/:filehash" [filehash]
       (do
         (update-metric "file-get")
         (handle-hash filehash)))

  (route/not-found
    (fn [req]
      (let [pic-index (random-in-range 1 4)]
        (render-file "templates/error-codes/404.html"
                     {:title-prefix "404"
                      :pic-index pic-index})))))
