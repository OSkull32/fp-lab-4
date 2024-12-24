(ns parser-combinators.parser)

(defn success [result rest]
  {:result result :rest rest})

(defn failure [error]
  {:error error})

(defn parse-char [c]
  (fn [input]
    (if (and (seq input) (= (first input) c))
      (success c (rest input))
      (failure (str "Expected '" c "', but found '" (first input) "'")))))

(defn string [s]
  (fn [input]
    (let [input-str (apply str input)] ; Ensure input is a string
      (if (.startsWith input-str s)
        (success s (subs input-str (count s)))
        (failure (str "Expected '" s "', but found '" (subs input-str 0 (min (count input-str) (count s))) "'"))))))

(defn regex [pattern]
  (fn [input]
    (let [input-str (apply str input)] ; Преобразуем input в строку
      (if-let [match (re-find (re-matcher pattern input-str))]
        (let [matched (if (coll? match) (first match) match)]
          (success matched (subs input-str (count matched))))
        (failure (str "Expected match for regex '" pattern "'"))))))

(defn choice [& parsers]
  (fn [input]
    (let [results (map #(% input) parsers)]
      (first (filter #(not (:error %)) results)))))

(defn many [parser]
  (fn [input]
    (loop [results [] rest-input input]
      (let [result (parser rest-input)]
        (if (:error result)
          (success results rest-input) ;; Если ошибка, возвращаем накопленные результаты
          (if (= rest-input (:rest result)) ;; Проверяем, изменился ли остаток
            (success results rest-input) ;; Если нет, избегаем зацикливания
            (recur (conj results (:result result)) (:rest result))))))))

(defn many1 [parser]
  (fn [input]
    (let [result ((many parser) input)]
      (if (empty? (:result result))
        (failure "Expected at least one match")
        result))))

(defn parse-seq [& parsers]
  (fn [input]
    (loop [ps parsers input input results []]
      (if (empty? ps)
        (success results input)
        (let [result ((first ps) input)]
          (if (:error result)
            result
            (recur (rest ps) (:rest result) (conj results (:result result)))))))))

(defn optional [parser]
  (fn [input]
    (let [result (parser input)]
      (if (:error result)
        (success nil input)
        result))))

