(ns parser-combinators.csv
  (:require [parser-combinators.parser :refer [many optional parse-char parse-seq regex]]))

;; Парсер для одной ячейки CSV
(defn parse-csv-cell []
  (fn [input]
    (let [result ((regex #"(?:[^,\\\n]|\\.)*") input)]
      (if (:error result)
        result
        (update result :result #(apply str %))))))

;; Парсер для строки CSV
(defn parse-csv-row []
  (fn [input]
    (if (empty? input)
      {:result [], :rest ""}
      (let [cell-parser (parse-csv-cell)
            comma-parser (optional (parse-char \,))
            row-parser (many (parse-seq cell-parser comma-parser))]
        (if (:error (row-parser input))
          (row-parser input)
          (update (row-parser input) :result #(map first %))))))) ;; Берем только результаты ячеек

;; Парсер для всего CSV
(defn parse-csv []
  (fn [input]
    (let [row-parser (parse-csv-row)
          newline-parser (optional (parse-char \newline))
          csv-parser (many (parse-seq row-parser newline-parser))]
      (if (:error (csv-parser input))
        (csv-parser input)
        (let [rows (map first (:result (csv-parser input)))]
          {:result (filter #(seq %) rows), :rest (:rest (csv-parser input))})))))

;; Функция для парсинга строки CSV
(defn parse-csv-string [input]
  (let [result ((parse-csv) input)]
    (if (:error result)
      (throw (Exception. (:error result)))
      (:result result))))

;; Функция для парсинга CSV из файла
(defn parse-csv-file [file-path]
  (let [content (slurp file-path)]
    (parse-csv-string content)))
