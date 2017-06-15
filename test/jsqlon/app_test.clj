(ns jsqlon.app-test
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.java.shell :as shell]
            [midje.sweet :refer :all]
            [jsqlon.main :refer :all]))

(def database-uris
  {"MySQL" "mysql://localhost/jsqlon_test?user=root&useSSL=false"
   "PostgreSQL" "postgresql://localhost/jsqlon_test?user=postgres"})

(def table-name (atom nil))

(against-background
 [(before :contents
          (let [result (shell/sh "docker-compose" "up" "databases")]
            (when (> (:exit result) 0)
              (throw (Exception. (str "Could not start the databases.\n"
                                      "STDOUT:\n" (:out result)
                                      "STDERR:\n" (:err result)))))))]

 (tabular
  (against-background
   [(before :facts
            (reset! table-name (str "test_" (rand-int 1000000))))
    (before :facts
            (jdbc/with-db-connection [db {:connection-uri (str "jdbc:" ?connection-uri)}]
              (jdbc/execute! db (jdbc/create-table-ddl @table-name
                                                       [[:name "varchar(255)" "not null"]
                                                        [:age "int"]]))))
    (after :facts
           (jdbc/with-db-connection [db {:connection-uri (str "jdbc:" ?connection-uri)}]
             (jdbc/execute! db (jdbc/drop-table-ddl @table-name))))]

   (fact "JSQLON can insert and query data"
         (jdbc/with-db-connection [db {:connection-uri (str "jdbc:" ?connection-uri)}]
           (run-query db (str "INSERT INTO " @table-name " VALUES ('Alice', 39), ('Bob', NULL)"))
           => [2]
           (run-query db (str "SELECT * FROM " @table-name))
           => [{:name "Alice", :age 39}
               {:name "Bob", :age nil}])))

  ?db-name     ?connection-uri
  "MySQL"      "mysql://localhost/jsqlon_test?user=root&useSSL=false"
  "PostgreSQL" "postgresql://localhost/jsqlon_test?user=postgres"))