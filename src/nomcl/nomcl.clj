(ns nomcl.nomcl)

(defn result [parsed-value remaining-input]
  {::value parsed-value ::input remaining-input}
  )

(defn log [o msg]
  (println msg o)
  o)

(defn update-value [v m]
  (-> v
      (update ::value m)))

(defn input [str]
  (vec str))

(defn unwrap [o]
  (::value o))

(defn remaining [o]
  (::input o))

(defn hex-digit? [c]
  (re-matches #"[0-9a-fA-F]" (str c)))

(defn discard-value [parser]
  (fn [input]
    (let [output (parser input)]
      {:input (:input output) :value nil})))
