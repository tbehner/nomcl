(ns ^{:doc "Choice combinators."
      :author "Timm Behner"}
    nomcl.branch
  (:require [nomcl.error :as error]))

(defn alt
  "Return the result of the first non-failing parser."
  [parsers]
  (fn [input]
    (let [parsers (map error/ignore-parsing-error parsers)]
      (first (filter #(not (nil? %)) (map #(% input) parsers))))))
