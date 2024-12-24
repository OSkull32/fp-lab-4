(ns parser-combinators.core-test
  (:require [clojure.test :refer :all]
            [parser-combinators.parser :refer :all]))

(deftest char-parser-matches-character
  (testing "char parser matches the character"
    (is (= ((parse-char \a) "abc") {:result \a :rest [\b \c]}))
    (is (= ((parse-char \a) "a") {:result \a :rest []}))))

(deftest char-parser-fails-on-mismatch
  (testing "char parser fails on character mismatch"
    (is (= (failure ((parse-char \a) "b")) {:error {:error "Expected 'a', but found 'b'"}}))))

(deftest test-regex
  (testing "regex parser"
    (is (= ((regex #"[a-z]+") "abc123") {:result "abc" :rest "123"}))
    (is (= ((regex #"[0-9]+") "123abc") {:result "123" :rest "abc"}))
    (is (= (failure ((regex #"[0-9]+") "abc")) {:error {:error "Expected match for regex '[0-9]+'"}}))))

(deftest test-seq
  (testing "seq parser"
    (is (= ((parse-seq (parse-char \a) (parse-char \b)) "abc") {:result [\a \b] :rest [\c]}))))

(deftest many-parser-matches-none
  (testing "many parser matches zero occurrences"
    (is (= ((many (parse-char \a)) "bc") {:result [] :rest "bc"}))))

(deftest many-parser-matches-multiple
  (testing "many parser matches multiple occurrences"
    (is (= ((many (parse-char \a)) "aaabc") {:result [\a \a \a] :rest [\b \c]}))))

(deftest many1-parser-matches-multiple
  (testing "many1 parser matches multiple occurrences"
    (is (= ((many1 (parse-char \a)) "aaabc") {:result [\a \a \a] :rest [\b \c]}))))

(deftest many1-parser-fails-on-none
  (testing "many1 parser fails when no match"
    (is (= (failure ((many1 (parse-char \a)) "bc")) {:error {:error "Expected at least one match"}}))))

(deftest choice-parses-first-matching-parser
  (testing "choice parses with the first matching parser"
    (is (= ((choice (parse-char \a) (parse-char \b)) "abc") {:result \a :rest [\b \c]}))
    (is (= ((choice (parse-char \a) (parse-char \b)) "bac") {:result \b :rest [\a \c]}))))

(deftest optional-parser-matches
  (testing "optional parser matches the character"
    (is (= ((optional (parse-char \a)) "abc") {:result \a :rest [\b \c]}))))

(deftest optional-parser-matches-none
  (testing "optional parser matches zero occurrences"
    (is (= ((optional (parse-char \a)) "bc") {:result nil :rest "bc"}))))

(run-tests)
