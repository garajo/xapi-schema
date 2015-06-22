(ns xapi-schema.dev.cljs
  (:require [cljs.build.api :as api]
            [clojure.java.io :as io]))


(def build-options
  {:optimizations  :none
   :output-to      "target/specs.js"
   :output-dir     "target/cljs"
   :cache-analysis true
   :source-map     true
   :pretty-print   true
   :verbose        true
   :watch-fn       (fn [] (println "Success!"))
   })

(defn run-specs []
  (let [process (.exec (Runtime/getRuntime) "phantomjs bin/speclj.js")
        output (.getInputStream process)
        error (.getErrorStream process)]
    (io/copy output (System/out))
    (io/copy error (System/err))
    (System/exit (.waitFor process))))

(defn -main [& args]
  (api/build "spec" build-options)
  (run-specs))
