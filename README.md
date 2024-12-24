# Лабораторная работа №4

---

**Выполнил:** Данченко Владимир Витальевич, 368087

**Группа:** P3334

---

# Задание:

Библиотека парсер комбинаторов. С разработанной библиотекой парсер комбинаторов реализовать:

1. Парсер json.
2. Потоковый парсер csv.

---

## Требования к разработанному ПО:

Программа должна быть реализована в функциональном стиле;

Требуется использовать идиоматичный для технологии стиль программирования;

Задание и коллектив должны быть согласованы;

Допустима совместная работа над одним заданием.

## Реализация

### Библиотека парсер комбинаторов

Парсер-комбинаторы — это функция, принимающая текстовый ввод и возвращающая результат разбора или ошибку. В Clojure удобным способом работы с парсер-комбинаторами является использование функций высшего порядка.

1. Определение структуры данных:

    Результат работы парсера - мапа: {:result <parsed-value> :rest <remaining-input>}.
    
    Обработка ошибок: {:error <error-message>}.
```clojure
(defn success [result rest]
  {:result result :rest rest})

(defn failure [error]
  {:error error})
```

2. Примитивные парсеры:

    char — парсер для одного символа.

    string — парсер для строки.

    regex — парсер по регулярному выражению.
```clojure
(defn parse-char [c]
  "Парсер для одного символа."
  (fn [input]
    (if (and (not (empty? input)) (= (first input) c))
      (success c (rest input))
      (failure (str "Expected '" c "', but found '" (first input) "'")))))

(defn string [s]
  "Парсер для строки."
  (fn [input]
    (let [input-str (apply str input)] ; Ensure input is a string
      (if (.startsWith input-str s)
        (success s (subs input-str (count s)))
        (failure (str "Expected '" s "', but found '" (subs input-str 0 (min (count input-str) (count s))) "'"))))))



(defn regex [pattern]
  "Парсер для регулярного выражения."
  (fn [input]
    (let [input-str (apply str input)] ; Преобразуем input в строку
      (if-let [match (re-find (re-matcher pattern input-str))]
        (let [matched (if (coll? match) (first match) match)]
          (success matched (subs input-str (count matched))))
        (failure (str "Expected match for regex '" pattern "'"))))))
```

3. Комбинаторы для комбинирования базовых парсеров:
    
    choice — пробует несколько парсеров.

    many — парсит повторяющиеся элементы.

    parse-seq — последовательно применяет несколько парсеров.

```clojure
(defn choice [& parsers]
  "Комбинатор, который пробует несколько парсеров по порядку."
  (fn [input]
    (let [results (map #(% input) parsers)]
      (first (filter #(not (:error %)) results)))))

(defn many [parser]
  "Комбинатор, который применяет парсер повторно (ноль или больше раз)."
  (fn [input]
    (loop [results [] rest-input input]
      (let [result (parser rest-input)]
        (if (:error result)
          (success results rest-input) ;; Если ошибка, возвращаем накопленные результаты
          (if (= rest-input (:rest result)) ;; Проверяем, изменился ли остаток
            (success results rest-input) ;; Если нет, избегаем зацикливания
            (recur (conj results (:result result)) (:rest result))))))))


(defn parse-seq [& parsers]
  (fn [input]
    (loop [ps parsers input input results []]
      (if (empty? ps)
        (success results input)
        (let [result ((first ps) input)]
          (if (:error result)
            result
            (recur (rest ps) (:rest result) (conj results (:result result)))))))))
```

### JSON-парсер

##### Структура JSON:

1. Базовые элементы:

    Числа.
    
    Строки.
    
    null, true, false.

```clojure
(defn number []
  (fn [input]
    (let [input-str (apply str input)]
      (let [m (re-matcher #"-?\d+(\.\d+)?([eE][+-]?\d+)?" input-str)]
        (if (.find m)
          (let [num (subs input-str (.start m) (.end m))]
            (success (read-string num) (subs input-str (.end m))))
          (failure "Expected number"))))))

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
```

2. Коллекции:

    Массивы.

    Объекты.

```clojure
(defn parse-array []
  (fn [input]
    (let [input-str (clojure.string/trim (apply str input))]
      (if (.startsWith input-str "[")
        (let [rest (subs input-str 1)]
          (loop [input rest results []]
            (let [input (clojure.string/trim input)]
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
```
3. Объединение:

    Общий парсер для JSON, объединяющий все возможные варианты

```clojure
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
```

### Потоковый CSV-парсер

Потоковый парсер CSV требует обработки построчно, что особенно удобно для реализации в Clojure

1. Базовый парсер строки:

```clojure
;; Парсер для одной ячейки CSV
(defn parse-csv-cell []
  (fn [input]
    (let [parser (regex #"(?:[^,\\\n]|\\.)*")] ;; Parse until a comma, backslash, or newline
      (let [result (parser input)]
        (if (:error result)
          result
          (update result :result #(apply str %))))))) ;; Convert result to string

;; Парсер для строки CSV
(defn parse-csv-row []
  (fn [input]
    (if (empty? input)
      {:result [], :rest ""}
      (let [cell-parser (parse-csv-cell)
            comma-parser (optional (parse-char \,))
            row-parser (many (parse-seq cell-parser comma-parser))]
        (let [result (row-parser input)]
          (if (:error result)
            result
            (update result :result #(map first %)))))))) ;; Берем только результаты ячеек
```
2. Потоковая обработка:

```clojure
;; Парсер для всего CSV
(defn parse-csv []
  (fn [input]
    (let [row-parser (parse-csv-row)
          newline-parser (optional (parse-char \newline))
          csv-parser (many (parse-seq row-parser newline-parser))]
      (let [result (csv-parser input)]
        (if (:error result)
          result
          (let [rows (map first (:result result))]
            {:result (filter #(not (empty? %)) rows), :rest (:rest result)}))))))

;; Функция для парсинга строки CSV
(defn parse-csv-string [input]
  (let [result ((parse-csv) input)]
    (if (:error result)
      (throw (Exception. (:error result)))
      (:result result))))
```

## Тестирование

--- 

Было написано большое количество тестов для каждого модуля, включая тесты на различные варианты входных данных и ошибочные входные данные


[core-test](test/parser_combinators/core_test.clj)

[json-test](test/parser_combinators/json_test.clj)

[csv-test](test/parser_combinators/csv_test.clj)


### Вывод тестов

```
Testing parser-combinators.core-test

Ran 11 tests containing 15 assertions.
0 failures, 0 errors.

Testing parser-combinators.core-test

Process finished with exit code 0
```

```
Testing parser-combinators.json-test

Ran 44 tests containing 61 assertions.
0 failures, 0 errors.

Testing parser-combinators.json-test

Process finished with exit code 0
```

```
Testing parser-combinators.csv-test

Ran 24 tests containing 24 assertions.
0 failures, 0 errors.

Testing parser-combinators.csv-test

Process finished with exit code 0
```

Вывод программы полностью совпадает с выводом в описание лабораторной работы

## Вывод

---

В результате выполнения лабораторной работы была создана эффективная и расширяемая библиотека для парсинга JSON и CSV, что подтверждается успешным прохождением всех тестов. Этот подход может быть использован в реальных проектах для обработки текстовых данных в различных форматах.
