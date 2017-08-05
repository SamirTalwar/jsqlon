(defproject jsqlon "0.2.0-SNAPSHOT"
  :description "Write SQL, read JSON."
  :url "https://github.com/SamirTalwar/jsqlon"
  :license {:name "GNU General Public License, Version 3"
            :url "https://www.gnu.org/licenses/gpl-3.0.en.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.cli "0.3.5"]
                 [com.fasterxml.jackson.core/jackson-databind "2.8.9"]
                 [com.github.jnr/jnr-unixsocket "0.18"]

                 [mysql/mysql-connector-java "6.0.6"]
                 [org.postgresql/postgresql "42.1.1"]]
  :main ^:skip-aot jsqlon.main
  :target-path "target/%s"
  :jvm-opts ["-Duser.timezone=UTC"]
  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[org.clojure/java.jdbc "0.7.0-alpha3"]
                                  [midje "1.8.3"]]
                   :plugins [[lein-midje "3.2.1"]]}})
