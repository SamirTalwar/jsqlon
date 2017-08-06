(ns jsqlon.json-test
  (:import java.io.StringReader
           [java.time LocalDate ZonedDateTime ZoneOffset]
           java.util.Date)
  (:require [midje.sweet :refer :all]
            [jsqlon.json :as json]))

(fact "parses arbitrary JSON as a Java data structure"
      (json/read-values (StringReader. "{
        \"a\": 1,
        \"b\": \"two\",
        \"she\": {
          \"sells\": [\"sea\", \"shells\"],
          \"on\": [\"the\", \"sea\", \"shore\"]
        },
        \"dee\": [\"dee\", \"dee\", \"dee\", \"DEE\", {\"pause\": 1}, \"de\", \"de\", \"de\", \"de\"]
      }"))
      => [{"a" 1
           "b" "two"
           "she" {"sells" ["sea" "shells"]
                  "on" ["the" "sea" "shore"]}
           "dee" ["dee" "dee" "dee" "DEE" {"pause" 1} "de" "de" "de" "de"]}])

(fact "parses a stream of arbitrary JSON structures as an iterator"
      (json/read-values (StringReader. "{\"thing\": 1}\n{\n\"thing\": 2\n}"))
      => [{"thing" 1}
          {"thing" 2}])

(fact "writes Java values to JSON"
      (json/write-str nil) => "null"
      (json/write-str 7) => "7"
      (json/write-str 3.5) => "3.5"
      (json/write-str "stuff") => "\"stuff\""
      (json/write-str (Date/from (.toInstant (ZonedDateTime/of 2017 8 1 19 0 0 0 ZoneOffset/UTC))))
      => "\"2017-08-01T19:00:00.000+0000\""
      (json/write-str (java.sql.Date. (.toEpochMilli (.toInstant (.atStartOfDay (LocalDate/of 1973 12 25) ZoneOffset/UTC)))))
      => "\"1973-12-25\""
      (json/write-str (ZonedDateTime/of 2003 6 30 12 34 56 789000000 ZoneOffset/UTC))
      => "\"2003-06-30T12:34:56.789Z\""
      (json/write-str [1 2 3]) => "[1,2,3]"
      (json/write-str {"a" 1, :b "two", \c 3.0}) => "{\"a\":1,\"b\":\"two\",\"c\":3.0}")
