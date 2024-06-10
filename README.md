# nomcl
Parser Combinator Library written in and for Clojure. This was heavily inspired by [nom](https://github.com/rust-bakery/nom), but not all functions are implemented yet.

## Example
```clojure
(defn from-hex [in]
  (Integer/parseInt (apply str in) 16))

(defn hex-color-parser [input]
  ((combinators/map-res (bytes/take-while-m-n 2 2 n/hex-digit?) from-hex) input))

(defn to-color [result]
  (let [[r g b] result]
    {:red r :green g :blue b}))

(defn parse-hex-color [input]
  (->> input
      ((bytes/tag [\#]))
      (n/remaining)
      ((combinators/map-value
        (multi/repeat-n 3 hex-color-parser)
        to-color))
  ))

(deftest  parse-hex-color-test
  (let [expected {:red 1 :green 2 :blue 3}
        input (vec "#010203")
        output (parse-hex-color input)]
    (is (= output expected))))
```
