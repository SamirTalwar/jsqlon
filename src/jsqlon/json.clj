(ns jsqlon.json
  (:import com.fasterxml.jackson.core.JsonFactory
           com.fasterxml.jackson.databind.ObjectMapper
           com.fasterxml.jackson.databind.module.SimpleModule
           com.fasterxml.jackson.databind.node.JsonNodeType
           com.fasterxml.jackson.databind.ser.std.StdSerializer))

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

(defn read-values [input]
  (let [object-reader (.reader mapper java.util.Map)
        iterator (.readValues object-reader input)]
    (take-while #(not= % nil) (repeatedly #(if (.hasNext iterator)
                                             (.next iterator)
                                             nil)))))

(defn read-str [json]
  (.readTree mapper json))

(defn write-str [value]
  (.writeValueAsString mapper value))
