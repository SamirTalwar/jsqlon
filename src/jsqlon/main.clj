(ns jsqlon.main
  (:gen-class)
  (import java.io.BufferedReader)
  (:require [clojure.java.jdbc :as jdbc]))

(defn run [connection-uri input]
  (jdbc/with-db-connection [db {:connection-uri (str "jdbc:" connection-uri)}]
    (doseq [query input]
      (println (jdbc/query db query)))))

(defn -main [connection-uri]
  (try
    (run connection-uri (line-seq (java.io.BufferedReader. *in*)))
    (catch Exception exception
      (.println *err* (.getMessage exception))
      (System/exit 1))))
