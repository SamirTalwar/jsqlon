(ns jsqlon.main
  (:gen-class)
  (:import java.io.BufferedReader
           java.sql.DriverManager)
  (:require [clojure.string :as string]
            [jsqlon.json :as json]))

(Class/forName "org.postgresql.Driver")

(defn run-query [connection sql]
  (let [statement (.prepareStatement connection sql)
        has-result-set (.execute statement)
        result-set (if has-result-set (.getResultSet statement) nil)
        results (if result-set (resultset-seq result-set) nil)]
    (json/write-str results)))

(defn run [connection-uri input]
  (with-open [connection (DriverManager/getConnection (str "jdbc:" connection-uri))]
    (doseq [sql input]
      (println (run-query connection sql)))))

(defn -main [connection-uri]
  (try
    (run connection-uri (line-seq (java.io.BufferedReader. *in*)))
    (catch Exception exception
      (.println *err* (.getMessage exception))
      (System/exit 1))))
