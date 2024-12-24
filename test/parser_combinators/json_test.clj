(ns parser-combinators.json-test
  (:require [clojure.test :refer :all]
            [parser-combinators.json :refer :all]))

(deftest number-parses-zero
  (testing "number parses zero"
    (is (= ((number) "0") {:result 0 :rest ""}))))

(deftest number-parses-negative-zero
  (testing "number parses negative zero"
    (is (= ((number) "-0") {:result 0 :rest ""}))))

(deftest number-parses-large-integer
  (testing "number parses large integer"
    (is (= ((number) "12345678901234567890") {:result 12345678901234567890 :rest ""}))))

(deftest number-parses-large-negative-integer
  (testing "number parses large negative integer"
    (is (= ((number) "-12345678901234567890") {:result -12345678901234567890 :rest ""}))))

(deftest number-parses-large-float
  (testing "number parses large float"
    (is (= ((number) "1234567890.1234567890") {:result 1234567890.1234567890 :rest ""}))))

(deftest number-parses-large-negative-float
  (testing "number parses large negative float"
    (is (= ((number) "-1234567890.1234567890") {:result -1234567890.1234567890 :rest ""}))))

(deftest number-parses-exponent-without-decimal
  (testing "number parses exponent without decimal"
    (is (= ((number) "1e10") {:result 1e10 :rest ""}))))

(deftest number-parses-negative-exponent-without-decimal
  (testing "number parses negative exponent without decimal"
    (is (= ((number) "-1e10") {:result -1e10 :rest ""}))))

(deftest number-parses-exponent-with-decimal
  (testing "number parses exponent with decimal"
    (is (= ((number) "1.23e10") {:result 1.23e10 :rest ""}))))

(deftest number-parses-negative-exponent-with-decimal
  (testing "number parses negative exponent with decimal"
    (is (= ((number) "-1.23e10") {:result -1.23e10 :rest ""}))))

(deftest json-string-parses-empty-string
  (testing "json string parses empty string"
    (is (= ((json-string) "\"\"") {:result "" :rest ""}))))

(deftest json-string-parses-string-with-escaped-backslash
  (testing "json string parses string with escaped backslash"
    (is (= ((json-string) "\"hello \\ world\"") {:result "hello \\ world" :rest ""}))))

(deftest json-string-parses-correctly
  (testing "json string parses correctly"
    (is (= ((json-string) "\"hello\"") {:result "hello" :rest ""}))
    (is (= ((json-string) "\"hello world\"") {:result "hello world" :rest ""}))))

(deftest json-string-fails-on-unterminated
  (testing "json string fails on unterminated string"
    (is (= ((json-string) "\"hello") {:error "Unterminated string"}))))

(deftest json-string-fails-on-non-string
  (testing "json string fails on non-string input"
    (is (= ((json-string) "hello") {:error "Expected string"}))))

(deftest parse-null-parses-correctly
  (testing "parse null parses correctly"
    (is (= ((parse-null) "null") {:result "null" :rest ""}))))

(deftest parse-null-fails-on-invalid-input
  (testing "parse null fails on invalid input"
    (is (= ((parse-null) "nul") {:error "Expected 'null', but found 'nul'"}))
    (is (= ((parse-null) "nulla") {:rest "a" :result "null"}))))

(deftest parse-true-parses-correctly
  (testing "parse true parses correctly"
    (is (= ((parse-true) "true") {:result true :rest ""}))))

(deftest parse-true-fails-on-invalid-input
  (testing "parse true fails on invalid input"
    (is (= ((parse-true) "tru") {:error "Expected 'true', but found 'tru'"}))
    (is (= ((parse-true) "truee") {:rest "e" :result true}))))

(deftest parse-false-parses-correctly
  (testing "parse false parses correctly"
    (is (= ((parse-false) "false") {:result false :rest ""}))))

(deftest parse-false-fails-on-invalid-input
  (testing "parse false fails on invalid input"
    (is (= ((parse-false) "fals") {:error "Expected 'false', but found 'fals'"}))
    (is (= ((parse-false) "falsee") {:rest "e" :result false}))))

(deftest parse-array-parses-empty-array
  (testing "parse array parses empty array"
    (is (= ((parse-array) "[]") {:result [] :rest ""}))))

(deftest parse-array-parses-array-with-mixed-elements
  (testing "parse array parses array with mixed elements"
    (is (= ((parse-array) "[null,true,123,\"value\"]") {:result ["null" true 123 "value"] :rest ""}))))

(deftest parse-array-parses-array-with-nested-objects
  (is (= ((parse-array) "[{\"key\":\"value\"}, {\"key2\":123}]")
         {:result [{"key" "value"} {"key2" 123}], :rest ""})))

(deftest parse-array-fails-on-invalid-element
  (testing "parse array fails on invalid element"
    (is (= ((parse-array) "[null,true,invalid,\"value\"]") {:error "Expected null, true, false, number, string, or object"}))))

(deftest parse-array-fails-on-missing-opening-bracket
  (testing "parse array fails on missing opening bracket"
    (is (= ((parse-array) "null,true,123,\"value\"]") {:error "Expected array"}))))

(deftest key-value-pair-parses-correctly
  (testing "key-value pair parses correctly"
    (is (= ((key-value-pair) "\"key\":\"value\"") {:result ["key" \: "value"] :rest ""}))
    (is (= ((key-value-pair) "\"key\":123") {:result ["key" \: 123] :rest ""}))
    (is (= ((key-value-pair) "\"key\": [null,true,123,\"value\"]") {:result ["key" \: ["null" true 123 "value"]] :rest ""}))
    (is (= ((key-value-pair) "\"key\":true") {:result ["key" \: true] :rest ""}))))

(deftest key-value-pair-fails-on-invalid-input
  (testing "key-value pair fails on invalid input"
    (is (= ((key-value-pair) "\"key\"123") {:error "Expected ':', but found '1'"}))
    (is (= ((key-value-pair) "key:value") {:error "Expected string"}))))

(deftest object-parses-correctly
  (testing "object parses correctly"
    (is (= ((object) "{\"key\": [null, true, 123, \"value\"]}") {:result {"key" ["null" true 123 "value"]} :rest ""}))))

(deftest object-fails-on-missing-bracket
  (testing "object fails on missing closing bracket"
    (is (= ((object) "{\"key\": [null, true, 123, \"value\"") {:error "Expected ':', but found ''"}))))

(deftest json-value-parses-empty-object
  (testing "json value parses empty object"
    (is (= ((json-value) "{}") {:result {} :rest ""}))))

(deftest json-value-parses-null
  (testing "json value parses null"
    (is (= ((json-value) "null") {:result "null" :rest ""}))))

(deftest json-value-parses-true
  (testing "json value parses true"
    (is (= ((json-value) "true") {:result true :rest ""}))))

(deftest json-value-parses-false
  (testing "json value parses false"
    (is (= ((json-value) "false") {:result false :rest ""}))))

(deftest json-value-parses-number
  (testing "json value parses number"
    (is (= ((json-value) "123") {:result 123 :rest ""}))
    (is (= ((json-value) "-123.45") {:result -123.45 :rest ""}))))

(deftest json-value-parses-string
  (testing "json value parses string"
    (is (= ((json-value) "\"hello\"") {:result "hello" :rest ""}))))

(deftest json-value-parses-nested-object
  (testing "json value parses nested object"
    (is (= ((json-value) "{\"key\":{\"nestedKey\":\"nestedValue\"}}") {:result {"key" {"nestedKey" "nestedValue"}} :rest ""}))))

(deftest json-value-parses-object
  (testing "json value parses object"
    (is (= ((json-value) "{\"key\":\"value\"}") {:result {"key" "value"} :rest ""}))))

(deftest json-value-parses-empty-array
  (testing "json value parses empty array"
    (is (= ((json-value) "[]") {:result [] :rest ""}))))

(deftest json-value-parses-array
  (testing "json value parses array"
    (is (= ((json-value) "[null,true,123,\"value\"]") {:result ["null" true 123 "value"] :rest ""}))))

(deftest json-value-fails-on-invalid-input
  (testing "json value fails on invalid input"
    (is (= ((json-value) "invalid") {:error "Expected null, true, false, number, string, or object"}))))

(deftest parse-json-file-parses-correctly
  (testing "parse json file parses correctly"
    (spit "test.json" "{\"key\":\"value\"}")
    (is (= (parse-json-file "test.json") {"key" "value"}))
    (spit "test.json" "[null,true,123,\"value\"]")
    (is (= (parse-json-file "test.json") ["null" true 123 "value"]))
    (spit "test.json" "123")
    (is (= (parse-json-file "test.json") 123))
    (spit "test.json" "\"string\"")
    (is (= (parse-json-file "test.json") "string"))
    (spit "test.json" true)
    (is (= (parse-json-file "test.json") true))
    (spit "test.json" false)
    (is (= (parse-json-file "test.json") false))
    (spit "test.json" "null")
    (is (= (parse-json-file "test.json") "null"))))

(deftest parse-json-file-fails-on-invalid-json
  (testing "parse json file fails on invalid json"
    (spit "test.json" "invalid")
    (is (thrown? Exception (parse-json-file "test.json")))))

(deftest parse-json-file-parses-complex-json
  (testing "parse json file parses complex JSON"
    (spit "test.json" "{\"key1\":{\"nestedKey1\":\"nestedValue1\",\"nestedKey2\":[1,2,3]},\"key2\":[{\"innerKey\":\"innerValue\"},true,false,null],\"key3\":123.45}")
    (is (= (parse-json-file "test.json")
           {"key1" {"nestedKey1" "nestedValue1"
                    "nestedKey2" [1 2 3]}
            "key2" [{"innerKey" "innerValue"} true false "null"]
            "key3" 123.45}))

    (spit "test.json" "{\"key1\":{\"nestedKey1\":\"nestedValue1\",\"nestedKey2\":[1,2,3,{\"deepKey\":\"deepValue\"}]},\"key2\":[{\"innerKey\":\"innerValue\"},true,false,null],\"key3\":123.45,\"key4\":{\"nestedKey3\":[{\"innerArrayKey\":\"innerArrayValue\"},[1,2,3],{\"innerObjectKey\":{\"deepNestedKey\":\"deepNestedValue\"}}]}}")
    (is (= (parse-json-file "test.json")
           {"key1" {"nestedKey1" "nestedValue1"
                    "nestedKey2" [1 2 3 {"deepKey" "deepValue"}]}
            "key2" [{"innerKey" "innerValue"} true false "null"]
            "key3" 123.45
            "key4" {"nestedKey3" [{"innerArrayKey" "innerArrayValue"}
                                  [1 2 3]
                                  {"innerObjectKey" {"deepNestedKey" "deepNestedValue"}}]}}))

    (spit "test.json" "{\"key1\":{\"nestedKey1\":\"nestedValue1\",\"nestedKey2\":[1,2,3,{\"deepKey\":\"deepValue\"}]},\"key2\":[{\"innerKey\":\"innerValue\"},true,false,null],\"key3\":123.45,\"key4\":{\"nestedKey3\":[{\"innerArrayKey\":\"innerArrayValue\"},[1,2,3],{\"innerObjectKey\":{\"deepNestedKey\":\"deepNestedValue\"}}]},\"key5\":[{\"arrayKey1\":\"arrayValue1\"},{\"arrayKey2\":\"arrayValue2\"},{\"arrayKey3\":[{\"deepArrayKey\":\"deepArrayValue\"}]}]}")
    (is (= (parse-json-file "test.json")
           {"key1" {"nestedKey1" "nestedValue1"
                    "nestedKey2" [1 2 3 {"deepKey" "deepValue"}]}
            "key2" [{"innerKey" "innerValue"} true false "null"]
            "key3" 123.45
            "key4" {"nestedKey3" [{"innerArrayKey" "innerArrayValue"}
                                  [1 2 3]
                                  {"innerObjectKey" {"deepNestedKey" "deepNestedValue"}}]}
            "key5" [{"arrayKey1" "arrayValue1"}
                    {"arrayKey2" "arrayValue2"}
                    {"arrayKey3" [{"deepArrayKey" "deepArrayValue"}]}]}))))

(run-tests)
