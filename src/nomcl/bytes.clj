(ns ^{:doc "Parsers recognizing streams of bytes."
     :author "Timm Behner"}
  nomcl.bytes
  (:require [nomcl.nomcl :as base]))

(defn -tag [t]
  (fn [input]
      (let [candidate (subvec input 0 (count t))]
        (if (= candidate t)
          (base/result (apply str candidate) (subvec input (count t)))
          (throw (Exception. (format "expected '%s', found '%s'" t candidate)))))))

(defn tag [t]
  (cond
    (vector? t) (-tag t)
    :else (-tag (vec t))))

(defn take-while-p [pred]
  (fn [input]
    (let [matching-input (take-while pred input)]
      (base/result (apply str matching-input) (subvec input (count matching-input))))))

(defn is_a [string]
  (let [letters (set string)]
    (take-while-p (fn [c] (contains? letters c)))))

(defn is_not [string]
  (let [letters (set string)]
    (println "letters not to match" letters)
    (take-while-p (fn [c] (not (contains? letters c))))))

(defn take-while-m-n [min max pred]
  (fn [input]
    (let [max-input (take max input)
          matching-input (take-while pred max-input)]
      (if (>= (count matching-input) min)
        (base/result (apply str matching-input) (subvec input (count matching-input)))
        (throw (Exception. (format "expected at least %d, found %d" min (count matching-input))))))))

(defn take [amount]
  (take-while-m-n amount amount #(true)))

(defn take-while-m [min pred]
  (fn [input]
    (let [matching-input (take-while pred input)]
      (if (>= (count matching-input) min)
        (base/result (apply str matching-input) (subvec input (count matching-input)))
        (throw (Exception. (format "expected at least %d, found %d" min (count matching-input))))))))

(defn take-while-1 [pred]
  (take-while-m 1 pred))

(defn take-till [pred]
  (fn [input]
    (let [matching-input (take-while (comp not pred) input)]
      (base/result (apply str matching-input) (subvec input (count matching-input))))))

(defn take-till-1 [pred]
  (fn [input]
    (let [out ((take-till pred) input)]
      (if (empty? (base/unwrap out))
        (throw (Exception. (format "expected at least one, nothing found")))
        out))))
