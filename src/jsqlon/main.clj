(ns jsqlon.main
  (:gen-class)
  (:import [java.io BufferedReader File IOException PrintWriter]
           [java.nio.channels Channels SelectionKey]
           java.sql.DriverManager
           [java.time LocalDate LocalDateTime LocalTime ZonedDateTime]
           jnr.enxio.channels.NativeSelectorProvider
           [jnr.unixsocket UnixServerSocket UnixServerSocketChannel UnixSocketAddress UnixSocketChannel])
  (:require [clojure.string :as string]
            [clojure.tools.cli :as cli]
            [jsqlon.json :as json]))

(Class/forName "org.postgresql.Driver")

(defn connect-to [connection-uri]
  (DriverManager/getConnection (str "jdbc:" connection-uri)))

(defn invalid-input-exception []
  (IllegalArgumentException. "Invalid input.\nFormat:\n{\"query\": \"SQL\", \"parameters\": [1, 'two', 3.0]}"))

(defn get-column-type [metadata i]
  {(keyword (.getColumnName metadata (+ i 1)))
   (string/lower-case (.getColumnTypeName metadata (+ i 1)))})

(defmulti transform-json-field type)
(defmethod transform-json-field nil [_]
  nil)
(defmethod transform-json-field String [value]
  (json/read-str value))
(defmethod transform-json-field org.postgresql.util.PGobject [pg-object]
  (json/read-str (.getValue pg-object)))
(defmethod transform-json-field :default [value]
  (throw (IllegalArgumentException. (str "Cannot convert a value of type " (type value) " to JSON."))))

(defn transform-field [column-types [field-name field-value]]
  {field-name (case (field-name column-types)
                "json" (transform-json-field field-value)
                field-value)})

(defn transform-row [column-types row]
  (into {} (map #(transform-field column-types %) row)))

(defn construct-statement [connection query parameters]
  (let [statement (.prepareStatement connection query)
        actual-parameter-count (count parameters)
        parameter-metadata (.getParameterMetaData statement)
        expected-parameter-count (.getParameterCount parameter-metadata)]
    (when (not= expected-parameter-count actual-parameter-count)
      (throw (IllegalArgumentException.
              (str "Expected " expected-parameter-count " parameters but got " actual-parameter-count "."))))
    (doseq [[i param] (map list (drop 1 (range)) parameters)]
      (.setObject statement i param))
    statement))

(defn run-query [connection query parameter-list]
  (try
    (when (not (string? query))
      (throw (invalid-input-exception)))
    (let [parameters (if (nil? parameter-list)
                       []
                       (try (vec parameter-list) (catch Exception e (throw (invalid-input-exception)))))
          statement (construct-statement connection query parameters)
          has-result-set (.execute statement)
          result-set (if has-result-set (.getResultSet statement) nil)]
      (if-not result-set
        (json/write-str {:success true})
        (let [metadata (.getMetaData result-set)
              column-count (.getColumnCount metadata)
              column-types (into {} (map #(get-column-type metadata %)
                                         (range column-count)))
              results (map #(transform-row column-types %) (resultset-seq result-set))]
          (json/write-str {:success true
                           :results results}))))
    (catch Exception e
      (json/write-str {:success false
                       :message (.getMessage e)}))))

(defn evaluate [connection request]
  (let [{query "query", parameters "parameters" :or {parameters []}}
        (.readValue json/mapper request java.util.Map)]
    (run-query connection query parameters)))

(defn with-stdin [behaviour]
  (doseq [request (line-seq (java.io.BufferedReader. *in*))]
    (println (behaviour request))))

(defn client-actor [channel behaviour]
  (fn []
    (let [reader (BufferedReader. (Channels/newReader channel "UTF-8"))
          writer (PrintWriter. (Channels/newWriter channel "UTF-8") true)]
      (doseq [request (line-seq reader)]
        (.println writer (behaviour request)))
      (.close channel))))

(defn server-actor [channel selector behaviour]
  (fn []
    (let [client (.accept channel)]
      (.configureBlocking client false)
      (.register client
                 selector
                 (bit-or SelectionKey/OP_READ SelectionKey/OP_WRITE)
                 (client-actor client behaviour)))))

(defn with-socket [socket-path behaviour]
  (let [socket-file (doto (File. socket-path) (.deleteOnExit))
        address (UnixSocketAddress. socket-file)
        channel (UnixServerSocketChannel/open)
        selector (.openSelector (NativeSelectorProvider/getInstance))]
    (do
      (.configureBlocking channel false)
      (.bind (.socket channel) address)
      (.register channel selector SelectionKey/OP_ACCEPT
                 (server-actor channel selector behaviour))

      (while (>= (.select selector) 0)
        (let [iterator (.iterator (.selectedKeys selector))]
          (doall (for [selected-key (iterator-seq iterator)]
                   (do
                     (.remove iterator)
                     ((.attachment selected-key))))))))))

(defn with-io [mode & args]
  (case mode
    :stdin with-stdin
    :socket #(with-socket (first args) %)))

(def cli-options
  [["-h" "--help"          "Shows this help text."]
   [nil  "--stdin"         "Read from STDIN, and write to STDOUT. (default)"]
   [nil  "--socket SOCKET" "Read and write to a Unix socket."]])

(defn print-usage [summary]
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

(defn -main [& args]
  (if-let [{connection-uri :connection-uri, io :io} (parse-opts args)]
    (with-open [connection (connect-to connection-uri)]
      ((apply with-io io) #(evaluate connection %)))
    (System/exit 1)))
