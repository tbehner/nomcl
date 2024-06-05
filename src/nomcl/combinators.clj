(ns ^{:doc "General combinators"
      :author "Timm Behner"}
 nomcl.combinators
  (:require [nomcl.nomcl :as base]
            [clojure.string :as string]))

(defn msg-log [o msg]
  (println msg o)
  o)

(defn map-res
  "Apply the mapper to the result of the parser."
  [parser mapper]
  (fn [input]
    (let [result (parser input)]
        (-> result
            (update ::base/value mapper)
            ))))

(defn map-value
  "Unwrap the result of the parser and apply mapper."
  [parser mapper]
  (fn [input]
    (mapper (base/unwrap (parser input)))))

(defn opt [parser]
  (fn [input]
    (try
      (parser input)
      (catch Exception _
        (base/result nil input)))))

(defn recognize
  "Return the string that is consumed by the parser as value instead of the value produced by the parser."
  [parser]
  (fn [input]
    (let [result (parser input)]
      (if (empty? (base/remaining result))
        (base/result (apply str input) [])
        (base/result (subvec input 0 (.start (::base/input result))) (base/remaining result))))))
