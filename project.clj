(defproject com.yetanalytics/xapi-schema "1.0.0-alpha5"
  :description "Clojure(script) Schema for the Experience API v1.0.3"
  :url "https://github.com/yetanalytics/xapi-schema"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.9.946"]
                 [cheshire "5.8.0"]]
  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-doo "0.1.8"]]

  :profiles {:dev {:dependencies [[org.clojure/tools.nrepl "0.2.10"]
                                  [com.cemerick/piggieback "0.2.2"]
                                  [org.clojure/test.check "0.10.0-alpha2"]]
                   :source-paths ["dev"]
                   :repl-options {:init-ns user
                                  :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}}

  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src"]
                        :compiler {:output-to "target/js/xapi_schema_dev.js"
                                   :optimizations :none
                                   :pretty-print true}}
                       {:id "test"
                        :source-paths ["src" "test"]
                        :main xapi-schema.runner
                        :compiler {:output-to "target/js/xapi_schema_test.js"
                                   :output-dir "target/js/test_out"
                                   :optimizations :whitespace
                                   :parallel-build true}}
                       {:id "release"
                        :source-paths ["src"]
                        :compiler {:output-to "target/js/xapi_schema.js"
                                   :optimizations :advanced}}]}
  :resource-paths ["resources"]
  :test-paths ["test"]
  :min-lein-version "2.6.1"
  :aliases {"deploy-lib" ["do" "clean," "deploy" "clojars"]
            "ci" ["do"
                  ["test"]
                  ["doo" "phantom" "test" "once"]]})
