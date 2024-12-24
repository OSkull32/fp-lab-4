(ns parser-combinators.json
  (:require [parser-combinators.parser  :refer [failure parse-char parse-seq string success]])
  (:require [clojure.string :as str]))

(declare json-value)

(defn number []
  (fn [input]
    (let [m (re-matcher #"-?\d+(\.\d+)?([eE][+-]?\d+)?" (apply str input))]
      (if (.find m)
        (let [num (subs (apply str input) (.start m) (.end m))]
          (success (read-string num) (subs (apply str input) (.end m))))
        (failure "Expected number")))))

(defn json-string []
  (fn [input]
    (let [input-str (apply str input)]
      (if (.startsWith input-str "\"")
        (let [end (loop [i 1]
                    (if (or (>= i (count input-str))
                            (and (= (nth input-str i) \")
                                 (not= (nth input-str (dec i)) \\)))
                      i
                      (recur (inc i))))]
          (if (and (>= end 0) (< end (count input-str)))
            (success (subs input-str 1 end) (subs input-str (inc end)))
            (failure "Unterminated string")))
        (failure "Expected string")))))

(defn parse-null []
  (string "null"))

(defn parse-true []
  (fn [input]
    (let [result ((string "true") input)]
      (if (:error result)
        result
        (success true (:rest result))))))

(defn parse-false []
  (fn [input]
    (let [result ((string "false") input)]
      (if (:error result)
        result
        (success false (:rest result))))))

(defn parse-array []
  (fn [input]
    (let [input-str (str/trim (apply str input))]
      (if (.startsWith input-str "[")
        (let [rest (subs input-str 1)]
          (loop [input rest results []]
            (let [input (str/trim input)]
              (cond
                (.startsWith input "]")
                (success results (subs input 1))

                (empty? input)
                (failure "Unexpected end of input in array")

                :else
                (let [result ((json-value) input)]
                  (if (:error result)
                    result
                    (let [next-input (clojure.string/trim (subs (:rest result) (if (.startsWith (:rest result) ",") 1 0)))]
                      (recur next-input (conj results (:result result))))))))))
        (failure "Expected array")))))

(defn key-value-pair []
  (fn [input]
    (let [result ((parse-seq (json-string) (parse-char \:) (json-value)) input)]
      (if (:error result)
        result
        (let [[k _ v] (:result result)]
          (success [k \: v] (:rest result)))))))

(defn object []
  (fn [input]
    (let [input-str (clojure.string/trim (apply str input))]
      (if (.startsWith input-str "{")
        (let [rest (subs input-str 1)]
          (loop [input rest result {}]
            (let [input (clojure.string/trim input)]
              (cond
                (.startsWith input "}")
                (success result (subs input 1))

                (empty? input)
                (failure "Unexpected end of input in object")

                :else
                (let [pair-result ((key-value-pair) input)]
                  (if (:error pair-result)
                    pair-result
                    (let [[k _ v] (:result pair-result)
                          next-input (clojure.string/trim (subs (:rest pair-result) (if (.startsWith (:rest pair-result) ",") 1 0)))]
                      (recur next-input (assoc result k v)))))))))
        (failure "Expected object")))))

(defn json-value []
  (fn [input]
    (let [parsers [(object) (parse-array) (json-string) (parse-null) (parse-true) (parse-false) (number)]]
      (loop [parsers parsers]
        (if (empty? parsers)
          (failure "Expected null, true, false, number, string, or object")
          (let [result ((first parsers) input)]
            (if (:error result)
              (recur (rest parsers))
              result)))))))

(defn parse-json-file [file-path]
  (let [result ((json-value) (slurp file-path))]
    (if (:error result)
      (throw (Exception. (:error result)))
      (:result result))))
