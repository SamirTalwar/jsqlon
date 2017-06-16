(ns jsqlon.json
  (:import [com.fasterxml.jackson.databind
            ObjectMapper
            module.SimpleModule
            node.JsonNodeType
            ser.std.StdSerializer]))

(def keyword-serializer
  (proxy [StdSerializer] [clojure.lang.Keyword]
    (serialize [value generator provider]
      (.writeFieldName generator (name value)))))

(def module
  (doto (SimpleModule.)
    (.addKeySerializer clojure.lang.Keyword keyword-serializer)))

(def mapper
  (doto (ObjectMapper.)
    (.registerModule module)))

(defn write-str [value]
  (.writeValueAsString mapper value))

(defn read-str [json]
  (let [tree (.readTree mapper json)
        node-type (.getNodeType tree)]
    (case (.name node-type)
      "ARRAY" (.convertValue mapper tree java.util.ArrayList)
      "BOOLEAN" (.convertValue mapper tree Boolean)
      "NULL" nil
      "NUMBER" (.convertValue mapper tree Double)
      "OBJECT" (.convertValue mapper tree java.util.Map)
      "STRING" (.convertValue mapper tree String))))
