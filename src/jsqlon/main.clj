(ns jsqlon.main
  (:gen-class)
  (:import java.io.BufferedReader)
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.string :as string]
            [jsqlon.json :as json]))

(defn run-query [db sql]
  (if (string/starts-with? (string/lower-case sql) "select ")
    (jdbc/query db sql)
    (jdbc/execute! db sql)))

(defn run [connection-uri input]
  (jdbc/with-db-connection [db {:connection-uri (str "jdbc:" connection-uri)}]
    (doseq [sql input]
      (println (json/write-str (run-query db sql))))))

(defn -main [connection-uri]
  (try
    (run connection-uri (line-seq (java.io.BufferedReader. *in*)))
    (catch Exception exception
      (.println *err* (.getMessage exception))
      (System/exit 1))))
