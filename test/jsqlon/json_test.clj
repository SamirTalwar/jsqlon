(ns jsqlon.json-test
  (:import java.io.StringReader)
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
