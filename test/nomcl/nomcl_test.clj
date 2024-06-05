(ns nomcl.nomcl-test
  (:require [clojure.test :refer :all]
            [nomcl.nomcl :refer :all :as n]
            [nomcl.branch :as branch]
            [nomcl.bytes :as bytes]
            [nomcl.character :as character]
            [nomcl.sequence :as sequence]
            [nomcl.multi :as multi]
            [nomcl.combinators :as combinators]
            ))

(defn from-hex [in]
  (Integer/parseInt (apply str in) 16))

(deftest  test-hex-digit-check
  (testing "conversion of a two char hex digit to int"
    (is (= 1 (from-hex '(\0 \1))))))

(deftest  take-while-n-m-test
  (testing "take while finds hex-digits"
    (is (= "01" (nomcl.nomcl/unwrap ( (bytes/take-while-m-n 2 2 n/hex-digit?) (n/input "01somethingelse")) )))))

(deftest  take-while-n-m-test-failure
  (testing "does not find enough"
    (is (thrown? Exception ((bytes/take-while-m-n 4 4 n/hex-digit?) (n/input "01nope"))))))

(deftest  take-while-1-test-failure
  (testing "does not find enough"
    (is (thrown? Exception ((bytes/take-while-1 n/hex-digit?) (vec "nope"))))))

(deftest  take-till-test
  (testing "finds the end"
    (is (= (n/result "nope" [\#]) ((bytes/take-till #(= \# %)) (vec "nope#"))))))

(deftest  take-till-test-no-end
  (testing "does not find the end"
    (is (= (n/result "nope;" []) ((bytes/take-till #(= \# %)) (vec "nope;"))))))

(deftest  take-till-1-test
  (testing "does not find enough"
    (is (thrown? Exception ((bytes/take-till-1 #(= \# %)) (vec "#"))))))

(deftest  take-while-p-test
  (testing "take while finds hex-digits"
    (is (= "01" (nomcl.nomcl/unwrap ( (bytes/take-while-p n/hex-digit?) (vec "01somethingelse")))))))

(deftest  take-while-m-test
  (testing "take while finds hex-digits"
    (is (= "01" (nomcl.nomcl/unwrap ( (bytes/take-while-m 2 n/hex-digit?) (vec "01somethingelse")))))))

(deftest  hex-string-test
  (testing "Parse a hex string and map to bytes."
    (is (= 1 (n/unwrap (
                        (combinators/map-res
                         (bytes/take-while-m-n 2 2 n/hex-digit?)
                         from-hex) (vec "01")))))))

(deftest  hex-string-test
  (testing "Parse a hex string and map to bytes."
    (is (= 1 ((combinators/map-value
                         (bytes/take-while-m-n 2 2 n/hex-digit?)
                         from-hex) (vec "01"))))))

(deftest  parse-tag
  (testing "Parse tags successfull"
    (is (= "#" (n/unwrap ((bytes/tag (vector \#)) (vec "#1234"))))))
  (testing "Parse tag unsuccessfull"
    (is (thrown? Exception ((bytes/tag (vector \#)) (vec "foo"))))))

(deftest  repeat-test
  (testing "repeating a single tag"
    (is (= ["#" "#" "#"] (n/unwrap ((multi/repeat-n 3 (bytes/tag "#")) (vec "###")))))))

(deftest  handle-failure-in-matches
  (testing "no failure, no remaining input"
    (let [expected (n/result [\#] (vector))
          input [(n/result \# (vector))]
          outcome (multi/collect-results input)]
      (is (= outcome expected)))))

(defn hex-color-parser [input]
  ((combinators/map-res (bytes/take-while-m-n 2 2 n/hex-digit?) from-hex) input))

(defn to-color [result]
  (let [[r g b] result]
    {:red r :green g :blue b}))


(defn parse-hex [input]
  (->> input
      ((bytes/tag [\#]))
      (n/remaining)
      ((combinators/map-value
        (multi/repeat-n 3 hex-color-parser)
        to-color))
  ))

(deftest  parse-complete-hex
  (let [expected {:red 1 :green 2 :blue 3}
        input (vec "#010203")
        output (parse-hex input)]
    (is (= output expected))))

(deftest  one-of-infix-operators
  (let [options (vec "+-*/")
        input (vec "*")
        expected \*
        outcome (n/unwrap ((character/one-of options) input))]
    (is (= expected outcome))))

(deftest  alt-tries-different-parsers
  (let [parsers [(bytes/tag "foo") (bytes/tag "bar") (bytes/tag "baz")]
        input (vec "baz")
        expected "baz"
        result (n/unwrap ((branch/alt parsers) input) )]
    (is (= result expected))))

(deftest  char-tag-finds-single-char
  (let [input (vec ":tag")
        parser (character/single-char \:)
        output (parser input)
        expected-value \:]
    (is (= expected-value (n/unwrap output)))))

(deftest  preceded-ignores-preceding
  (let [input (vec ":tag")
        expected "tag"
        parser (sequence/preceded (character/single-char \:) (bytes/tag "tag"))
        result (parser input)]
    (is (= (n/unwrap result) expected))))

(deftest  digit1-parses-a-digit
  (let [input (vec "123cm")
        expected "123"
        output (character/digit1 input)]
    (is (= (n/unwrap output) expected))))

(deftest  optional-parser-matching
  (let [input (vec "tag")
        expected "tag"
        output ((combinators/opt (bytes/tag "tag")) input)]
    (is (= expected (n/unwrap output)))))

(deftest  optional-parser-matching-does-not-match
  (let [input (vec "other-than-that")
        output ((combinators/opt (bytes/tag "tag")) input)]
    (is (= nil (n/unwrap output)))))

(deftest  apply-multiple-parsers
  (let [foo-parser (bytes/tag "foo")
        colon-parser (character/single-char \:)
        input (vec "foo:1")
        expected ["foo" \: "1"]
        output (multi/p-> input
                      foo-parser
                      colon-parser
                      character/digit1)]
    (is (= expected (n/unwrap output)))))

(deftest  delimited-with-backets
  (let [input (vec "(1)")
        parser (sequence/delimited (character/single-char \() character/digit1 (character/single-char \)))
        output (parser input)
        expected (n/result "1" [])]
    (is (= expected output))))

(deftest  comma-separated-list
  (let [input (vec "1,2,3,42,1337")
        parser (sequence/separated-list character/digit1 (character/single-char \,))
        output (parser input)
        expected-values ["1" "2" "3" "42" "1337"]]
    (is (= output (n/result expected-values [])))))

(deftest  whitespace-test
  (let [input (vec " \tfoo")
        result (character/whitespace1 input)
        expected (n/result " \t" (vec "foo"))
        ]
  (is (= result expected))))

(deftest  whitespace-separated-list
  (let [input (vec "1 2 3 42 1337")
        parser (sequence/separated-list character/digit1 character/whitespace1)
        output (parser input)
        expected-values ["1" "2" "3" "42" "1337"]]
    (is (= output (n/result expected-values [])))))

(deftest  s-expr-list-test
  (let [input (vec "'(somefun :param otherparam)")
        tick-parser (character/single-char \')
        open-par (character/single-char \()
        word-parser (bytes/take-while-p (character/re? #"[-a-zA-Z]"))
        keywords (sequence/preceded (character/single-char \:) character/word1)
        vars (sequence/separated-list (branch/alt [keywords word-parser]) character/whitespace1)
        closing-par (character/single-char \))
        parser (sequence/delimited open-par vars closing-par)
        output (-> (multi/p-> input
                          tick-parser
                          parser)
                   (update ::n/value #(get % 1)))
        expected (n/result ["somefun" "param" "otherparam"] [])]
    (is (= output expected))))

(deftest  parse-boolean-values
  (let [
        true-parser (combinators/map-value (bytes/tag ":true") (fn[_] ::true))
        false-parser (combinators/map-value (bytes/tag ":false") (fn [_] ::false))
        boolean-value-parser (branch/alt [true-parser false-parser])
        tests [":true" (n/result ::true [])
               ":false" (n/result ::false [])]]
    (for [[input expected] tests]
      (is (= (boolean-value-parser input) expected)))))

(deftest  terminated-parser-test
  (let [input (vec "expression;other" )
        parser (sequence/terminated character/word1 (character/single-char \;))
        expected (n/result ["expression"] (vec "other"))
        output (parser input)]
    (is (= output expected))))

(deftest  many1-finds-multiple-results
  (is (= ( (multi/many1
            (character/single-char \.)
            ) (vec "...boom"))
         (n/result [\. \. \.] (vec "boom")))))

(deftest  many1-throws-if-none-is-found
  (is (thrown? Exception ((multi/many1 (character/single-char \.)) (vec ",,,boom")))))

(deftest  many0-finds-multiple-results
  (is (= ( (multi/many0
            (character/single-char \.)
            ) (vec "...boom" ))
         (n/result [\. \. \.] (vec "boom")))))

(deftest  many0-does-not-throw-if-none-found
  (is (= ( (multi/many0
            (character/single-char \.)
            ) (vec ",,,boom"))
         (n/result [] (vec ",,,boom")))))

(deftest  recognize-returns-string-as-value
  (let [input (vec "123abc")
        parser (combinators/recognize (multi/many1 (character/one-of "0123456789")) )
        expect (n/result (vec "123") (vec "abc"))
        output (parser input)]
    (is (= expect output))))

(deftest  many0-matches-on-empty-string
  (let [input (vector) 
        parser (multi/many0 (character/single-char \_))
        output (parser input)
        expected (n/result [] [])]
    (is (= output expected))))

(deftest  terminated-handles-optional-terminator-gracefully
  (let [input (vec "1234")
        parser (sequence/terminated (character/one-of "01234567") (multi/many0 (character/single-char \_)))
        output (parser input)
        expected (n/result [\1] (vec "234"))]
    (is (= output expected))))

(def octal-example-parser
  (sequence/preceded
   (branch/alt [(bytes/tag "0o") (bytes/tag "0O")])
   (combinators/recognize
    (multi/many1
     (sequence/terminated
      (character/one-of "01234567") (multi/many0 (character/single-char \_)))))))

(deftest  octal-parser-test
  (let [input (vec "0o1234")
        expected (n/result "1234" [])
        output (octal-example-parser input)
        ]
    (is (= expected output))))

(deftest ^:failing pair-fails-if-any-fails
  (let [input (vec "12foo")
        parser (sequence/pair character/digit1 character/whitespace1)]
    (is (thrown? Exception (parser input)))))

(def integer-number-parser
  (combinators/map-res
   character/digit1
   #(Integer/parseInt (apply str %) 10)))

(def numbers-sep-with-whitespace-parser
   (sequence/separated-list integer-number-parser character/whitespace1))

(deftest ^:failing numbers-with-whitespace-parser-test
  (let [input (vec "12 6 10 foo")
        expected (n/result [12 6 10] (vec " foo"))
        output (numbers-sep-with-whitespace-parser input)]
    (is (= output expected))))

(def numbers-with-name-parser
  (sequence/separated-pair
   numbers-sep-with-whitespace-parser
   character/whitespace1
   character/word1))

(deftest pair-matches-exactly-both-parsers
  (let [input (vec "12 ")
        expected (n/result ["12" " "] [])
        parser (sequence/pair character/digit1 character/whitespace1)
        output (parser input)
        ]
    (is (= output expected))))

(deftest numbers-with-name-parser-test
  (let [input (vec "12 6 10 foo")
        expected (n/result [[12 6 10] "foo"] (vector))
        output (numbers-with-name-parser input)]
    (is (= output expected))))

(deftest separated-pair-test
  (let [input (vec "12 foo")
        expected (n/result [12 "foo"] [])
        parser (sequence/separated-pair integer-number-parser character/whitespace1 character/word1)
        output (parser input)]
    (is (= output expected))
    ))

(deftest is-a-hex-test
  (let [input (vec "123 and viola")
        parser (bytes/is_a "0123456789")
        expected (n/result "123" (vec " and viola"))
        output (parser input)]
    (is (= output expected))))

(deftest is-not-space-test
  (is (= ((bytes/is_not " \t\r\n") (vec "Hello, World!" )) (n/result "Hello," (vec " World!")))))
