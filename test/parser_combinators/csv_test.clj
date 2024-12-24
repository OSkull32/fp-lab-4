(ns parser-combinators.csv-test
  (:require [clojure.test :refer :all]
            [parser-combinators.csv :refer :all]))

(deftest parse-csv-cell-empty
  (testing "Parsing empty CSV cell"
    (is (= ((parse-csv-cell) "") {:result "", :rest ""}))))

(deftest parse-csv-cell-single-word
  (testing "Parsing CSV cell with a single word"
    (is (= ((parse-csv-cell) "cell") {:result "cell", :rest ""}))))

(deftest parse-csv-cell-with-comma
  (testing "Parsing CSV cell with a comma"
    (is (= ((parse-csv-cell) "cell,rest") {:result "cell", :rest ",rest"}))))

(deftest parse-csv-cell-with-newline
  (testing "Parsing CSV cell with a newline"
    (is (= ((parse-csv-cell) "cell\nrest") {:result "cell", :rest "\nrest"}))))

(deftest parse-csv-cell-with-special-characters
  (testing "Parsing CSV cell with special characters"
    (is (= ((parse-csv-cell) "cell!@#") {:result "cell!@#", :rest ""}))))

(deftest parse-csv-row-empty
  (testing "Parsing empty CSV row"
    (is (= ((parse-csv-row) "") {:result [], :rest ""}))))

(deftest parse-csv-row-single-cell
  (testing "Parsing CSV row with a single cell"
    (is (= ((parse-csv-row) "cell") {:result ["cell"], :rest ""}))))

(deftest parse-csv-row-multiple-cells
  (testing "Parsing CSV row with multiple cells"
    (is (= ((parse-csv-row) "cell1,cell2,cell3") {:result ["cell1" "cell2" "cell3"], :rest ""}))))

(deftest parse-csv-row-with-newline
  (testing "Parsing CSV row with a newline"
    (is (= ((parse-csv-row) "cell1,cell2,cell3\n") {:result ["cell1" "cell2" "cell3"], :rest "\n"}))))

(deftest parse-csv-row-with-special-characters
  (testing "Parsing CSV row with special characters"
    (is (= ((parse-csv-row) "cell1!@#,cell2$%^,cell3&*()") {:result ["cell1!@#" "cell2$%^" "cell3&*()"], :rest ""}))))

(deftest parse-csv-row-with-empty-cell
  (testing "Parsing CSV row with an empty cell"
    (is (= ((parse-csv-row) "cell1,,cell3") {:result ["cell1" "" "cell3"], :rest ""}))))

(deftest parse-csv-row-with-leading-and-trailing-spaces
  (testing "Parsing CSV row with leading and trailing spaces"
    (is (= ((parse-csv-row) " cell1 , cell2 , cell3 ") {:result [" cell1 " " cell2 " " cell3 "], :rest ""}))))

(deftest parse-csv-row-with-quoted-cells
  (testing "Parsing CSV row with quoted cells"
    (is (= ((parse-csv-row) "\"cell1\",\"cell2\",\"cell3\"") {:result ["\"cell1\"" "\"cell2\"" "\"cell3\""], :rest ""}))))

(deftest parse-csv-row-with-mixed-quoted-and-unquoted-cells
  (testing "Parsing CSV row with mixed quoted and unquoted cells"
    (is (= ((parse-csv-row) "\"cell1\",cell2,\"cell3\"") {:result ["\"cell1\"" "cell2" "\"cell3\""], :rest ""}))))

(deftest parse-csv-row-with-escaped-characters
  (testing "Parsing CSV row with escaped characters"
    (is (= ((parse-csv-row) "cell1\\,cell2\\,cell3") {:result ["cell1\\,cell2\\,cell3"], :rest ""}))))

(deftest parse-csv-string-empty
  (testing "Parsing empty CSV string"
    (is (= (parse-csv-string "") []))))

(deftest parse-csv-string-single-cell
  (testing "Parsing CSV string with a single cell"
    (is (= (parse-csv-string "cell") [["cell"]]))))

(deftest parse-csv-string-single-row
  (testing "Parsing CSV string with a single row"
    (is (= (parse-csv-string "a,b,c") [["a" "b" "c"]]))))

(deftest parse-csv-string-multiple-rows
  (testing "Parsing CSV string with multiple rows"
    (is (= (parse-csv-string "a,b,c\n1,2,3\nx,y,z")
           [["a" "b" "c"]
            ["1" "2" "3"]
            ["x" "y" "z"]]))))

(deftest parse-csv-string-trailing-newline
  (testing "Parsing CSV string with trailing newline"
    (is (= (parse-csv-string "a,b,c\n1,2,3\n")
           [["a" "b" "c"]
            ["1" "2" "3"]]))))

(deftest parse-csv-string-with-escaped-characters
  (testing "Parsing CSV string with escaped characters"
    (is (= (parse-csv-string "cell1\\,cell2\\,cell3")
           [["cell1\\,cell2\\,cell3"]]))))

(deftest parse-csv-string-with-quoted-cells
  (testing "Parsing CSV string with quoted cells"
    (is (= (parse-csv-string "\"cell1\",\"cell2\",\"cell3\"")
           [["\"cell1\"" "\"cell2\"" "\"cell3\""]]))))

(deftest parse-csv-string-with-mixed-quoted-and-unquoted-cells
  (testing "Parsing CSV string with mixed quoted and unquoted cells"
    (is (= (parse-csv-string "\"cell1\",cell2,\"cell3\"")
           [["\"cell1\"" "cell2" "\"cell3\""]]))))

(deftest parse-csv-string-with-special-characters
  (testing "Parsing CSV string with special characters"
    (is (= (parse-csv-string "cell1!@#,cell2$%^,cell3&*()")
           [["cell1!@#" "cell2$%^" "cell3&*()"]]))))

(run-tests)

(deftest test-parse-csv-file
  (testing "Parsing CSV file"
    (spit "test.csv" "name,age\nAlice,30\nBob,25")
    (is (= (parse-csv-file "test.csv")
           [["name" "age"]
            ["Alice" "30"]
            ["Bob" "25"]]))))
