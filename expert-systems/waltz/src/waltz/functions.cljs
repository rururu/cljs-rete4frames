(ns waltz.functions
  (:require-macros [waltz.macro :as macro]))

(defn get-y [val]
  (mod val 100))

(defn get-x [val]
  (int (/ val 100)))

(defn get_angle [?p1 ?p2]
  (let [?delta-x (- (get-x ?p2) (get-x ?p1))
        ?delta-y (- (get-y ?p2) (get-y ?p1))]
    (if (= ?delta-x 0)
      (if (> ?delta-y 0)
        (/ (aget js/Math "PI") 2)
        (/ (aget js/Math "PI") -2))
      (if (= ?delta-y 0)
        (if (> ?delta-x 0)
          0.0
          (aget js/Math "PI"))
        (.atan2 js/Math ?delta-y ?delta-x)))))

(defn inscribed_angle [?basepoint ?p1 ?p2]
  (let [?angle1 (get_angle ?basepoint ?p1)
        ?angle2 (get_angle ?basepoint ?p2)
        ?temp0 (- ?angle1 ?angle2)
        ?temp1 (if (< ?temp0 0) (- 0 ?temp0) ?temp0)
        ?temp2 (if (> ?temp1 (aget js/Math "PI")) (- (* 2 (aget js/Math "PI")) ?temp1) ?temp1)]
    (if (< ?temp2 0)
      (- 0 ?temp2)
      ?temp2)))

(defn make_3_junction [?basepoint ?p1 ?p2 ?p3]
  (let [?angle12 (inscribed_angle ?basepoint ?p1 ?p2)
        ?angle13 (inscribed_angle ?basepoint ?p1 ?p3)
        ?angle23 (inscribed_angle ?basepoint ?p2 ?p3)
        ?sum1213 (+ ?angle12 ?angle13)
        ?sum1223 (+ ?angle12 ?angle23)
        ?sum1323 (+ ?angle13 ?angle23)
        [?sum ?shaft ?barb1 ?barb2]
          (if (< ?sum1213 ?sum1223)
            (if (< ?sum1213 ?sum1323)
              [?sum1213 ?p1 ?p2 ?p3]
              [?sum1323 ?p3 ?p1 ?p2])
            (if (< ?sum1223 ?sum1323)
              [?sum1223 ?p2 ?p1 ?p3]
              [?sum1323 ?p3 ?p1 ?p2]))
        ?delta0 (- ?sum (aget js/Math "PI"))
        ?delta1 (if (< ?delta0 0) (- 0 ?delta0) ?delta0)
        ?type (if (< ?delta1 0.001)
                'tee
                (if (> ?sum (aget js/Math "PI"))
                  'fork
                  'arrow))
        p1 (int ?barb1)
        p2 (int ?shaft)
        p3 (int ?barb2)
        bp (int ?basepoint)
        frm (list 'junct 'p1 p1 'p2 p2 'p3 p3
          'base-point bp 'type ?type)]
    (waltz.core/assert-frame frm)))

(macro/create-eval-for ['get-y get-y
                        'get-x get-x
                        'get_angle get_angle
                        'inscribed_angle inscribed_angle
                        'make_3_junction make_3_junction
                        'not= not=
                        'not-exists waltz.core/not-exists
                        'asser waltz.core/asser
                        'modify waltz.core/modify
                        'retract waltz.core/retract
                        'println println
                        'str str
                        '> >
                        '= =
                        '< <
                        '>= >=
                        '<= <=])
