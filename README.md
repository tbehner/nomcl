# nomcl
Parser Combinator Library written in and for Clojure. This was heavily inspired by [nom](https://github.com/rust-bakery/nom), but not all functions are implemented yet.

## Example
```clojure
(defn from-hex [in]
  (Integer/parseInt (apply str in) 16))

(defn hex-color-parser [input]
  ((combinators/map-res (bytes/take-while-m-n 2 2 nomcl/hex-digit?) from-hex) input))

(defn to-color [result]
  (let [[r g b] result]
    {:red r :green g :blue b}))

(defn parse-hex-color [input]
  (->> input
      ((bytes/tag [\#]))
      (nomcl/remaining)
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

## Progress

| combinator                                                                       | usage                       | input           | output                    | comment                                                                                                                                                                                                                                                                  |
|----------------------------------------------------------------------------------|-----------------------------|-----------------|---------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [single-char](https://tbehner.github.io/nomcl/nomcl.character.html#var-single-char)           | `(single-char 'a')`                 | `(vec "abc")`         | `{:input (vec "bc"), :value \a}`         | Matches one character (works with non ASCII chars too)                                                                                                                                                                                                                   |
| [is_a](https://tbehner.github.io/nomcl/nomcl.bytes.html#var-is_a)               | `(is_a "ab")`                | `"abbac"`       | `{:input (vec "c"), :value "abba"}`       | Matches a sequence of any of the characters passed as arguments                                                                                                                                                                                                          |
| [is_not](https://tbehner.github.io/nomcl/nomcl.bytes.html#var-is_not)           | `(is_not "cd")`              | `"ababc"`       | `{:input (vec "c"), :value "abab"}`       | Matches a sequence of none of the characters passed as arguments                                                                                                                                                                                                         |
| [one-of](https://tbehner.github.io/nomcl/nomcl.character.html#var-one-of)       | `(one_of "abc")`             | `"abc"`         | `{:input (vec "bc" ), :value \a }`         | Matches one of the provided characters (works with non ASCII characters too)                                                                                                                                                                                             |
| [none_of](https://docs.rs/nom/latest/nom/character/complete/fn.none_of.html)     | `none_of("abc")`            | `"xyab"`        | `Ok(("yab", 'x'))`        | Matches anything but the provided characters                                                                                                                                                                                                                             |
| [tag](https://tbehner.github.io/nomcl/nomcl.bytes.html#var-tag)                 | `(tag "hello")`              | `"hello world"` | `{:input (vec " world"), :value "hello"}` | Recognizes a specific suite of characters or bytes                                                                                                                                                                                                                       |
| [tag_no_case](https://docs.rs/nom/latest/nom/bytes/complete/fn.tag_no_case.html) | `tag_no_case("hello")`      | `"HeLLo World"` | `Ok((" World", "HeLLo"))` | Case insensitive comparison. Note that case insensitive comparison is not well defined for unicode, and that you might have bad surprises                                                                                                                                |
| [take](https://tbehner.github.io/nomcl/nomcl.bytes.html#var-take)               | `(take 4)`                   | `"hello"`       | `{:input (vec "o"), :value "hell"}`       | Takes a specific number of bytes or characters                                                                                                                                                                                                                           |
| [take-while-p](https://tbehner.github.io/nomcl/nomcl.bytes.html#var-take-while-p)   | `(take-while-p is_alphabetic)` | `"abc123"`      | `{:input (vec "123"), :value "abc"}`      | Returns the longest list of bytes for which the provided function returns true. `take_while1` does the same, but must return at least one character, while `take_while_m_n` must return between m and n                                                                  |
| [take-till](https://tbehner.github.io/nomcl/nomcl.bytes.html#var-take-till)     | `(take_till is_alphabetic)`  | `"123abc"`      | `{:input (vec "abc"), :value "123"}`      | Returns the longest list of bytes or characters until the provided function returns true. `take_till1` does the same, but must return at least one character. This is the reverse behaviour from `take_while`: `take_till(f)` is equivalent to `take_while(\|c\| !f(c))` |
| [take_until](https://docs.rs/nom/latest/nom/bytes/complete/fn.take_until.html)   | `take_until("world")`       | `"Hello world"` | `Ok(("world", "Hello "))` | Returns the longest list of bytes or characters until the provided tag is found. `take_until1` does the same, but must return at least one character                                                                                                                     |

