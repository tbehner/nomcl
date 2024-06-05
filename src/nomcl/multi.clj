(ns ^{:doc "Combinators applying their child parsers multiple times."
      :author "Timm Behner"}
    nomcl.multi
  (:require [nomcl.nomcl :as base]))

(defn results
  ( []
   {::values (vector) ::input ""})
  ( [input]
   {::values (vector) ::input input})
  ( [values input]
   {::values values ::input input} ))

(defn repeat-parser [parser acc n]
  (let [match (parser (::input acc))]
    (-> acc
        (assoc ::input (base/remaining match))
        (update ::values #(conj % match)))))

(defn collect-results
  "Collect the values from a vec of results to a result with a vec of values."
  [matches]
  (base/result (vec (map base/unwrap matches)) (base/remaining (last matches))))

;; TODO StringIndexOutOfBoundsException catchen und in entsprechenden Fehler umwandeln (input reicht nicht) 
(defn repeat-n [count parser]
  (fn [input]
    (let [out (reduce (partial repeat-parser parser)
                      (results [] input)
                     (range count))]
    (base/result (vec (map base/unwrap (::values out))) (::input out)))))

(defn apply-parser [result parser]
  (try
    (let [output (parser (::input result))]
      (-> result
          (update ::values #(conj % output))
          (assoc ::input (::base/input output))))
    (catch Exception e (reduced result))))

(defn apply-parsers [input parsers]
  (let [combined-results (reduce apply-parser (results input) parsers)]
    (base/result (vec (map base/unwrap (::values combined-results))) (::input combined-results))))

(defn p-> [input & parsers]
  (apply-parsers input parsers))

(defn ->p [parsers]
  (fn [input]
    (apply-parsers input parsers)))

(defn many1 [parser]
  (fn [input]
    (let [result (apply-parsers input (cycle [parser]))]
      (if (>= (count (::base/value result)) 1)
        (do
          result)
        (throw (Exception. (format "expected at least one, got none")))))))


(defn many0 [parser]
  (fn [input]
    (let [result (apply-parsers input (cycle [parser]))]
        result
        )))
