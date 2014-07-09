(ns waltz.rules)

(def RULES '(
(r-reverse-edges 0
	(stage value duplicate)
	?l (line p1 ?p1 p2 ?p2)
	=>
	(asser edge p1 ?p1 p2 ?p2 joined FALSE label NIL plotted NIL)
  (asser edge p1 ?p2 p2 ?p1 joined FALSE label NIL plotted NIL)
	(retract ?l))

(r-done-reversing -10
	?s (stage value duplicate)
	=>
  (println (str " FIRE: done-reversing"))
	(modify ?s value detect-junctions))

(r-make_3_junction 10
	(stage value detect-junctions)
	?e1 (edge p1 ?base-point p2 ?p1 joined FALSE)
	?e2 (edge p1 ?base-point p2 ?p2 joined FALSE
        (> ?p1 ?p2))
	?e3 (edge p1 ?base-point p2 ?p3 joined FALSE
        ((> ?p2 ?p3)
         (not= ?p1 ?p3)))
	=>
	(modify ?e1 joined TRUE)
	(modify ?e2 joined TRUE)
	(modify ?e3 joined TRUE)
  	(make_3_junction ?base-point ?p1 ?p2 ?p3))

(r1-make-L 0
	(stage value detect-junctions)
	?e1 (edge p1 ?base-point p2 ?p2 joined FALSE)
	?e2 (edge p1 ?base-point p2 ?p3 joined FALSE
        ((> ?p3 ?p2)
         (not-exists edge p1 ?base-point)))
	=>
	(modify ?e1 joined TRUE)
	(modify ?e2 joined TRUE)
	(asser junct type L
               	base-point ?base-point
		        p1 ?p2
		        p2 ?p3))

(r2-make-L 0
	(stage value detect-junctions)
	?e1 (edge p1 ?base-point p2 ?p2 joined FALSE)
	?e2 (edge p1 ?base-point p2 ?p3 joined FALSE
        (> ?p3 ?p2))
	(edge p1 ?base-point p2 ?p4
        ((not= ?e2 ?p3)
         [(= ?p4 ?p2) (= ?p4 ?p3)]))
	=>
	(modify ?e1 joined TRUE)
	(modify ?e2 joined TRUE)
	(asser junct type L
               	base-point ?base-point
		        p1 ?p2
		        p2 ?p3))

(r-done-detecting -10
	?s (stage value detect-junctions)
	=>
  (println (str " FIRE: done-detecting"))
	(modify ?s value find-limits))

(r-find-limits-max 5
  (stage value find-limits)
  ?lim (limits max ?max)
  (junct base-point ?bp (> ?bp ?max))
  =>
  (modify ?lim max ?bp))

(r-find-limits-min 5
  (stage value find-limits)
  ?lim (limits min ?min)
  (junct base-point ?bp (< ?bp ?min))
  =>
  (modify ?lim min ?bp))

(r-done-find-limits 0
  ?s (stage value find-limits)
    =>
  (println (str " FIRE: done-find-limits"))
  (modify ?s value find-initial-boundary))

(r-initial-boundary-junction-L 0
	?s (stage value find-initial-boundary)
  (limits max ?max)
  (junct type L
              base-point ?base-point
              p1 ?p1
              p2 ?p2
        (>= ?base-point ?max))
	?e1 (edge p1 ?base-point p2 ?p1)
	?e2 (edge p1 ?base-point p2 ?p2)
	=>
  (println (str " FIRE: done find-initial-boundary"))
  (modify ?e1 label B)
	(modify ?e2 label B)
	(modify ?s value find-second-boundary))

(r-initial-boundary-junction-arrow 0
	?s (stage value find-initial-boundary)
  (limits max ?max)
	(junct type arrow base-point ?bp p1 ?p1 p2 ?p2 p3 ?p3
        (>= ?bp ?max))
	?e1 (edge p1 ?bp p2 ?p1)
	?e2 (edge p1 ?bp p2 ?p2)
	?e3 (edge p1 ?bp p2 ?p3)
	=>
  (println (str " FIRE: done find-initial-boundary"))
  (modify ?e1 label B)
	(modify ?e2 label PLUS)
	(modify ?e3 label B)
	(modify ?s value find-second-boundary))

(r-second-boundary-junction-L 0
	?s (stage value find-second-boundary)
  (limits min ?min)
  (junct type L base-point ?bp p1 ?p1 p2 ?p2
       (<= ?bp ?min))
	?e1 (edge p1 ?base-point p2 ?p1)
	?e2 (edge p1 ?base-point p2 ?p2)
	=>
  (println (str " FIRE: done find-second-boundary"))
  (modify ?e1 label B)
  (modify ?e2 label B)
  (modify ?s value labeling))

(r-second-boundary-junction-arrow 0
	?s (stage value find-second-boundary)
  (limits min ?min)
	(junct type arrow base-point ?bp p1 ?p1 p2 ?p2 p3 ?p3
        (<= ?bp ?min))
	?e1 (edge p1 ?bp p2 ?p1)
	?e2 (edge p1 ?bp p2 ?p2)
	?e3 (edge p1 ?bp p2 ?p3)
	=>
  (println (str " FIRE: done find-second-boundary"))
  (modify ?e1 label B)
	(modify ?e2 label PLUS)
	(modify ?e3 label B)
	(modify ?s value labeling))

(r-match-edge 0
	(stage value labeling)
	?e1 (edge p1 ?p1 p2 ?p2 label ?l
        [(= ?l PLUS) (= ?l MINUS) (= ?l B)])
	?e2 (edge p1 ?p2 p2 ?p1 label NIL)
	=>
	(modify ?e1 plotted T)
	(modify ?e2 label ?l plotted T))

(r-label-L 0
	(stage value labeling)
	(junct type L base-point ?p1)
	(edge p1 ?p1 p2 ?p2 label ?l
        [(= ?l PLUS) (= ?l MINUS)])
	?e2 (edge p1 ?p1 p2 ?p3 label NIL
        (not= ?p3 ?p2))
	=>
	(modify ?e2 label B))

(r-label-tee-A 5
	(stage value labeling)
	(junct type tee base-point ?bp p1 ?p1 p2 ?p2 p3 ?p3)
	?e1 (edge p1 ?bp p2 ?p1 label NIL)
	?e2 (edge p1 ?bp p2 ?p3)
	=>
  (modify ?e1 label B)
	(modify ?e2 label B))

(r-label-tee-B 0
	(stage value labeling)
	(junct type tee base-point ?bp p1 ?p1 p2 ?p2 p3 ?p3)
	?e1 (edge p1 ?bp p2 ?p1)
	?e2 (edge p1 ?bp p2 ?p3 label NIL)
	=>
   (modify ?e1 label B)
	(modify ?e2 label B))

(r-label-fork-1 0
	(stage value labeling)
	(junct type fork base-point ?bp)
	(edge p1 ?bp p2 ?p1 label PLUS)
	?e2 (edge p1 ?bp p2 ?p2 label NIL
        (not= ?p2 ?p1))
	?e3 (edge p1 ?bp p2 ?p3
        ((not= ?p3 ?p2)
         (not= ?p3 ?p1)))
	=>
	(modify ?e2 label PLUS)
	(modify ?e3 label PLUS))

(r-label-fork-2 0
	(stage value labeling)
	(junct type fork base-point ?bp)
	(edge p1 ?bp p2 ?p1 label B)
	(edge p1 ?bp p2 ?p2 label MINUS
        (not= ?p2 ?p1))
	?e3 (edge p1 ?bp p2 ?p3 label NIL
        ((not= ?p3 ?p2)
         (not= ?p3 ?p1)))
	=>
	(modify ?e3 label B))

(r-label-fork-3 0
	(stage value labeling)
	(junct type fork base-point ?bp)
	(edge p1 ?bp p2 ?p1 label B)
	(edge p1 ?bp p2 ?p2 label B
        (not= ?p2 ?p1))
	?e3 (edge p1 ?bp p2 ?p3 label NIL
        ((not= ?p3 ?p2)
         (not= ?p3 ?p1)))
	=>
	(modify ?e3 label MINUS))

(r-label-fork-4 0
	(stage value labeling)
	(junct type fork base-point ?bp)
	(edge p1 ?bp p2 ?p1 label MINUS)
	(edge p1 ?bp p2 ?p2 label MINUS
        (not= ?p2 ?p1))
	?e3 (edge p1 ?bp p2 ?p3 label NIL
        ((not= ?p3 ?p2)
         (not= ?p3 ?p1)))
	=>
	(modify ?e3 label MINUS))

(r-label-arrow-1A 5
	(stage value labeling)
	(junct type arrow base-point ?bp p1 ?p1 p2 ?p2 p3 ?p3)
	(edge p1 ?bp p2 ?p1 label ?l
        [(= ?l B) (= ?l MINUS)])
	?e2 (edge p1 ?bp p2 ?p2 label NIL)
	?e3 (edge p1 ?bp p2 ?p3)
	=>
	(modify ?e2 label PLUS)
	(modify ?e3 label ?l))

(r-label-arrow-1B 0
	(stage value labeling)
	(junct type arrow base-point ?bp p1 ?p1 p2 ?p2 p3 ?p3)
	(edge p1 ?bp p2 ?p1 label ?l
        [(= ?l MINUS) (= ?l B)])
	?e2 (edge p1 ?bp p2 ?p2)
	?e3 (edge p1 ?bp p2 ?p3 label NIL)
	=>
	(modify ?e2 label PLUS)
	(modify ?e3 label ?l))

(r-label-arrow-2A 5
	(stage value labeling)
	(junct type arrow base-point ?bp p1 ?p1 p2 ?p2 p3 ?p3)
	(edge p1 ?bp p2 ?p3 label ?l
        [(= ?l B) (= ?l MINUS)])
	?e2 (edge p1 ?bp p2 ?p2 label NIL)
	?e3 (edge p1 ?bp p2 ?p1)
	=>
	(modify ?e2 label PLUS)
	(modify ?e3 label ?l))

(r-label-arrow-2B 0
	(stage value labeling)
	(junct type arrow base-point ?bp p1 ?p1 p2 ?p2 p3 ?p3)
	(edge p1 ?bp p2 ?p3 label ?l
        [(= ?l B) (= ?l MINUS)])
	?e2 (edge p1 ?bp p2 ?p2)
	?e3 (edge p1 ?bp p2 ?p1 label NIL)
	=>
	(modify ?e2 label PLUS)
	(modify ?e3 label ?l))

(r-label-arrow-3A 5
	(stage value labeling)
	(junct type arrow base-point ?bp p1 ?p1 p2 ?p2 p3 ?p3)
	(edge p1 ?bp p2 ?p1 label PLUS)
	?e2 (edge p1 ?bp p2 ?p2 label NIL)
	?e3 (edge p1 ?bp p2 ?p3)
	=>
	(modify ?e2 label MINUS)
	(modify ?e3 label PLUS))

(r-label-arrow-3B 0
	(stage value labeling)
	(junct type arrow base-point ?bp p1 ?p1 p2 ?p2 p3 ?p3)
	(edge p1 ?bp p2 ?p1 label PLUS)
	?e2 (edge p1 ?bp p2 ?p2)
	?e3 (edge p1 ?bp p2 ?p3 label NIL)
	=>
	(modify ?e2 label MINUS)
	(modify ?e3 label PLUS))

(r-label-arrow-4A 5
	(stage value labeling)
	(junct type arrow base-point ?bp p1 ?p1 p2 ?p2 p3 ?p3)
	(edge p1 ?bp p2 ?p3 label PLUS)
	?e2 (edge p1 ?bp p2 ?p2 label NIL)
	?e3 (edge p1 ?bp p2 ?p1)
	=>
	(modify ?e2 label MINUS)
	(modify ?e3 label PLUS))

(r-label-arrow-4B 0
	(stage value labeling)
	(junct type arrow base-point ?bp p1 ?p1 p2 ?p2 p3 ?p3)
	(edge p1 ?bp p2 ?p3 label PLUS)
	?e2 (edge p1 ?bp p2 ?p2)
	?e3 (edge p1 ?bp p2 ?p1 label NIL)
	=>
	(modify ?e2 label MINUS)
	(modify ?e3 label PLUS))

(r-label-arrow-5A 5
	(stage value labeling)
	(junct type arrow base-point ?bp p1 ?p1 p2 ?p2 p3 ?p3)
	(edge p1 ?bp p2 ?p2 label MINUS)
	?e2 (edge p1 ?bp p2 ?p1)
	?e3 (edge p1 ?bp p2 ?p3 label NIL)
	=>
	(modify ?e2 label PLUS)
	(modify ?e3 label PLUS))

(r-label-arrow-5B 0
	(stage value labeling)
	(junct type arrow base-point ?bp p1 ?p1 p2 ?p2 p3 ?p3)
	(edge p1 ?bp p2 ?p2 label MINUS)
	?e2 (edge p1 ?bp p2 ?p1 label NIL)
	?e3 (edge p1 ?bp p2 ?p3)
	=>
	(modify ?e2 label PLUS)
	(modify ?e3 label PLUS))

(r-done-labeling -10
	?s (stage value labeling)
	=>
  (println (str " FIRE: done-labeling"))
	(modify ?s value plot-boundaries))

(r-plot-boundaries 5
	(stage value plot-boundaries)
	?e (edge plotted NIL label NIL p1 ?p1 p2 ?p2)
	=>
	(modify ?e plotted TRUE))

(r-done-plot-boundaries 0
	?s (stage value plot-boundaries)
	=>
  (println (str " FIRE: done-plot-boundaries"))
	(modify ?s value plot-remaining-edges))

(r-plot-remaining-edges 5
    (stage value plot-remaining-edges)
    ?e (edge plotted NIL)
    =>
    (modify ?e plotted TRUE))

(r-done-plotting 0
	?s (stage value plot-remaining-edges)
	=>
  (println (str " FIRE: done-plotting"))
	(modify ?s value done))
))
