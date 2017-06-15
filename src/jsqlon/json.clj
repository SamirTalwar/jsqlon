(ns jsqlon.json
  (:import [com.fasterxml.jackson.databind ObjectMapper module.SimpleModule ser.std.StdSerializer]))

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
  (.readValue mapper json java.util.ArrayList))
