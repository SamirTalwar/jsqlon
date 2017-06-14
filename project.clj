(defproject jsqlon "0.1.0-SNAPSHOT"
  :description "Write SQL, read JSON."
  :url "https://github.com/SamirTalwar/jsqlon"
  :license {:name "GNU General Public License, Version 3"
            :url "https://www.gnu.org/licenses/gpl-3.0.en.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]]
  :main ^:skip-aot jsqlon.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
