(ns mab.macro)

(defmacro create-eval-for [pairs]
  (let [pp (partition 2 pairs)]
    `(defn ~'eval-in-ctx [~'e ~'ctx]
       (if (and (seq? ~'e) (symbol? (first ~'e)))
         (condp = (first ~'e)
           ~@(mapcat (fn [[n f]] [n `(apply ~f (map #(~'eval-in-ctx (or (get ~'ctx %) %) ~'ctx) (rest ~'e)))]) pp))
         ~'e))))

