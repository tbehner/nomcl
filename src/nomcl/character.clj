(ns ^{:doc "Character specific parsers and combinators."
      :author "Timm Behner"}
    nomcl.character
  (:require [nomcl.error :as error]
            [nomcl.nomcl :as base]
            [nomcl.bytes :as bytes]))

(defn single-char [c]
  (fn [input]
    (if (= c (first input))
      (base/result c (subvec input 1))
      (throw (Exception. (format "expected '%s', found '%s'" c (first input)))))))

(defn one-of [options]
  (fn [input]
    (let [parsers (map error/ignore-parsing-error (map single-char options))
          results (map #(% input) parsers)]
      (if (some #(not (nil? %)) results)
        (first (filter #(not (nil? %)) results ))
        (throw (Exception. (format "expected one of '%s', found '%s'" options (first input))))))))
        

(defn digit? [c]
  (re-matches #"[0-9]" (str c)))

(defn re? [r]
  (fn [c]
    (re-matches r (str c))))

(defn digit1 [input]
  ((bytes/take-while-1 digit?) input))

(defn whitespace1 [input]
  ((bytes/take-while-1 (re? #"\s")) input))

(defn word1 [input]
  ((bytes/take-while-1 (re? #"\w")) input))
