(ns sharchan.core-test
  (:require [clojure.test :refer :all]
            [sharchan.core :refer :all]
            [sharchan.util :refer :all]))

(deftest encoding-decoding-test
  (let [test-cases [[0 "m"]
                    [1 "n"]
                    [62 "3bm"]
                    [12345 "3idarWH"]
                    [456789 "GIc6WxD7"]
                    [999999 "Hrhk7Mmv"]]]
    (doseq [[num encoded] test-cases]
(let [encoded-result (encode (.getBytes (str num)))
      decoded-result (String. (decode encoded))]

  (println "Number:" num)
  (println "Encoded:" encoded-result)
  (println "Decoded:" decoded-result)

  (is (= encoded encoded-result))
  (is (= (str num) decoded-result))))))
