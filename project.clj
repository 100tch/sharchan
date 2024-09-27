(defproject sharchan "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://github.com/100tch/sharchan"
  :license {:name "BSD-2-Clause"
            :url "https://opensource.org/license/BSD-2-Clause"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/core.async "1.6.681"]
                 [ring/ring-core "1.9.6"]
                 [ring/ring-jetty-adapter "1.9.6"]
                 [ring/ring-defaults "0.3.3"]
                 [compojure "1.6.2"]
                 [selmer "1.12.51"]
                 [com.github.seancorfield/next.jdbc "1.3.939"]
                 [org.xerial/sqlite-jdbc "3.41.2.1"]
                 [digest "1.4.10"]]
  :plugins [[lein-asset-minifier "0.4.7"]]
  :minify-assets [[:css {:target "resources/public/static/min/css/styles.min.css"     :source ["resources/public/static/css/"]}]
                  [:js {:target  "resources/public/static/min/js/scripts-head.min.js" :source "resources/public/static/js/head/"}]
                  [:js {:target  "resources/public/static/min/js/scripts.min.js"      :source "resources/public/static/js/common/"}]]
  :main ^:skip-aot sharchan.core
  :target-path "target/%s"
  :ring {:handler sharchan.core/app}
  :repl-options {:init-ns sharchan.core}
  :profiles {:uberjar {:aot :all
                       :prep-tasks ["compile" ["minify-assets" "minify"]]
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}
             :dev {:prep-tasks [["minify-assets" "minify"]]}
             :dev-wfc {:prep-tasks []}})
