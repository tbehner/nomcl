(ns nomcl.sequence
  (:require [nomcl.nomcl :as base]
            [nomcl.multi :as multi]))

(defn preceded [pre parser]
  (fn [input]
    (-> input
        (pre)
        (base/remaining)
        (parser))))

(defn delimited [left-del inner right-del]
  (fn [input]
    (let [results (multi/p-> input
                            left-del
                            inner
                            right-del)]
      (-> results
          (update ::base/value #(get % 1))))))

(defn terminated
  "Apply a parser and the parser of the terminator.
  Throws an error if the result is empty (parser and terminator did not match)."
  [element-parser terminator-parser]
  (fn [input]
    (let [results (multi/apply-parsers input [element-parser terminator-parser])]
      (if (empty? (base/unwrap results))
        (throw (Exception. (format "Expected element, found nothing.")))
        (-> results
            (update ::base/value #(drop-last %)))))))


(defn n-tuple [& parsers]
  (fn [input]
    (let [results (multi/apply-parsers input parsers)]
      (if (not= (count parsers) (count (::base/value results)))
        (throw (Exception. (format "Expected tuple, found only %s" "something")))
        results
        ))))

(defn pair [first second]
  (n-tuple first second))

(defn separated-pair [left sep-parser right]
  (fn [input]
    (let [results (multi/apply-parsers input [left sep-parser right])]
      (if (some nil? (::base/value results))
        (throw (Exception. (format "separated-pair did not match all fields")))
        (-> results
            (update ::base/value #(vector (first %) (last %))))))))

(defn separated-list [element-parser sep-parser]
  (fn [input]
    (let [parsers (concat [element-parser] (repeat (pair sep-parser element-parser)))
          results (multi/apply-parsers input parsers)]
      (-> results
          (update ::base/value #(vec (concat (vector (first %) ) (map (fn [t] (get t 1)) (rest %) ))))))))
