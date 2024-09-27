(ns sharchan.core
  (:gen-class)
  (:require [sharchan.db :refer [create-tables]]
            [sharchan.routes :refer [app-routes]]
            [sharchan.config :refer [config]]
            [sharchan.util :refer [ensure-storage-dir]]
            [sharchan.cleanup :refer [start-periodic-cleanup]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]))

;; app init
(def app
  (-> app-routes
      (wrap-params)
      (wrap-multipart-params)
      (wrap-resource "public")
      (wrap-defaults (assoc-in site-defaults [:security :anti-forgery] false))))

(defn -main [& args]
  (create-tables)
  (ensure-storage-dir)
  (start-periodic-cleanup 30000)
  (run-jetty app {:port (config :port) :join? false}))
