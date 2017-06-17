(ns jsqlon.main
  (:gen-class)
  (:import java.io.BufferedReader
           java.sql.DriverManager
           [java.time LocalDate LocalDateTime LocalTime ZonedDateTime])
  (:require [clojure.string :as string]
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

(defn run [connection-uri input]
  (with-open [connection (connect-to connection-uri)]
    (doseq [request input]
      (let [{query "query", parameters "parameters" :or {parameters []}} (.readValue json/mapper request java.util.Map)]
        (println (run-query connection query parameters))))))

(defn -main [connection-uri]
  (try
    (run connection-uri (line-seq (java.io.BufferedReader. *in*)))
    (catch Exception exception
      (.println *err* (.getMessage exception))
      (System/exit 1))))
