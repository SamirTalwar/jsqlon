(ns jsqlon.cli
 (:require [clojure.tools.cli :as cli]))

(def cli-options
  [["-h" "--help"          "Shows this help text."]
   [nil  "--stdin"         "Read from STDIN, and write to STDOUT. (default)"]
   [nil  "--socket SOCKET" "Read and write to a Unix socket."]])

(defn- print-usage [summary]
  (do
    (println "Usage: jsqlon [OPTIONS] CONNECTION-URI")
    (println)
    (println "Options:")
    (println summary)))

(defn parse-opts [args]
  (let [{[connection-uri] :arguments, options :options, summary :summary, errors :errors}
        (cli/parse-opts args cli-options)]
    (cond
      errors
      (do
        (print-usage summary)
        (println)
        (println "Errors:")
        (doseq [error errors]
          (println "  - " error))
        nil)

      (nil? connection-uri)
      (do
        (print-usage summary)
        nil)

      (and (:stdin options) (:socket options))
      (do
        (print-usage summary)
        (println)
        (println "Errors:")
        (println "  - Requires only one of `--stdin` or `--socket`.")
        nil)

      (:help options)
      (print-usage summary)

      :else
      {:connection-uri connection-uri
       :io (if (:socket options) [:socket (:socket options)] [:stdin])})))
