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

(defn read-value [json]
  (.readValue mapper json java.util.Map))

(defn read-str [json]
  (.readTree mapper json))

(defn write-str [value]
  (.writeValueAsString mapper value))
