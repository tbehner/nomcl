(ns nomcl.error)


(defn ignore-parsing-error [parser]
  (fn [input]
    (try
      (parser input)
      (catch Exception _
        nil))))
