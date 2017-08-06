(ns jsqlon.json-test
  (:require [midje.sweet :refer :all]
            [jsqlon.json :as json]))

(fact "parses arbitrary JSON as a Java data structure"
      (json/read-value "{
        \"a\": 1,
        \"b\": \"two\",
        \"she\": {
          \"sells\": [\"sea\", \"shells\"],
          \"on\": [\"the\", \"sea\", \"shore\"]
        },
        \"dee\": [\"dee\", \"dee\", \"dee\", \"DEE\", {\"pause\": 1}, \"de\", \"de\", \"de\", \"de\"]
      }")
      => {"a" 1
          "b" "two"
          "she" {"sells" ["sea" "shells"]
                 "on" ["the" "sea" "shore"]}
          "dee" ["dee" "dee" "dee" "DEE" {"pause" 1} "de" "de" "de" "de"]})
