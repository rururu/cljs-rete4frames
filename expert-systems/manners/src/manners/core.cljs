(ns manners.core
  (:require [manners.templates :as temps]
            [manners.rules :as rules]
            [manners.functions :as f]
            [manners.facts :as facts]))

(enable-console-print!)

(declare TEMPL ACNT ANET AMEM)

(def TRACE nil)

(defn vari? [x]
  "Is x variable?"
  (and (symbol? x) (= (.substr (name x) 0 1) "?")))

(defn prod-name [prod]
  "Get name of production"
  (first prod))

(defn salience [prod]
  "Get salience of production"
  (second prod))

(defn lhs [prod]
  "Get left hand side of production"
  (nnext (take-while #(not (=  % '=>)) prod)))

(defn rhs [prod]
  "Get right hand side of production"
  (rest (drop-while #(not (=  % '=>)) prod)))

(defn dissoc-in
  "Dissociates an entry from a nested associative structure returning a new
  nested structure. keys is a sequence of keys. Any empty maps that result
  will not be present in the new structure. (clojure.incubator)"
  [m [k & ks :as keys]]
  (if ks
    (if-let [nextmap (get m k)]
      (let [newmap (dissoc-in nextmap ks)]
        (if (seq newmap)
          (assoc m k newmap)
          (dissoc m k)))
      m)
    (dissoc m k)))

(defn templ-map [slots]
  "Created sorted map from list of slots with '? values"
  (let [itl (interleave slots (repeat '?))]
    (apply sorted-map itl)))

(defn mapmem-put [typ mp value mem]
  "Put into tree-like from map made memory"
  (let [templ (TEMPL typ)
        canon (cons typ (vals (merge templ mp)))]
    (reset! mem (assoc-in @mem canon value))))

(defn mapmem-get [typ mp mem]
  "Get from tree-like from map made memory"
  (let [templ (TEMPL typ)
        canon (cons typ (vals (merge templ mp)))]
    (get-in @mem canon)))

(defn mapmem-rem [typ mp mem]
  "Remove from tree-like from map made memory"
  (let [templ (TEMPL typ)
        canon (cons typ (vals (merge templ mp)))]
    (reset! mem (dissoc-in @mem canon))))

(defn template
  "Select template part of condition"
  [condition]
  (if (even? (count condition))
    (butlast condition)
    condition))

(defn mk-typmap [frame]
  "Create Typmap (list of type and map) from frame (list of type and keys with values)"
  (let [[typ & rst] frame
        mp (apply hash-map rst)]
    (list typ mp)))

(defn univars [cond]
  "Reduces all different variables to '?"
  (map #(if (vari? %) '? %) cond))

(defn add-anet-entry
  "If condition in a left hand side of a rule is a pattern (not test with a function on the predicate place)
  adds a new entry to the map representing the alpha net.@
  If test contains call to 'not-exist', for its args also adds entries"
  ([condition]
   (let [[typ mp] (mk-typmap (univars (template condition)))]
     (when (nil? (mapmem-get typ mp ANET))
       (mapmem-put typ mp @ACNT ANET)
       (swap! ACNT inc)))))

(defn anet-for-pset [pset]
  "Build the alpha net for the given production set (rule set) <pset> as a map"
  ;;(println [:ANET-FOR-PSET pset])
  (doseq [pp pset]
    (if TRACE (println [:PRODUCTION pp]))
    (doseq [condition (lhs pp)]
		(if TRACE (println [:condition condition]))
	    (add-anet-entry condition)) ))

(defn a-indexof-pattern [pattern]
  "Find an alpha memory cell index for a pattern from a left hand side of some rule"
  ;;(println [:A-INDEXOF-PATTERN :PATTERN pattern])
  (let [[typ mp] (mk-typmap (univars pattern))]
    (mapmem-get typ mp ANET)))

(defn collect-vars
  "Returns vector of variables in expression"
  ([ex]
   (vec (set (collect-vars ex nil))))
  ([ex yet]
   (cond
    (and (or (seq? ex) (vector? ex) (= (type ex) cljs.core/PersistentHashSet))
         (not (empty? ex)))
      (collect-vars (first ex) (collect-vars (rest ex) yet))
    (vari? ex) (cons ex yet)
    true yet)))

(defn qq [aa vars]
  "Add call to quote for symbols not variables"
  (let [f1 (fn [x]
             (if (and (symbol? x)
                      (not (or (vari? x) (some #{x} vars))))
               (list 'quote x)
               x))]
    (map f1 aa)
    aa))

(defn mk-pattern-and-test [condition]
  "Make pattern or test"
  ;;(println [:MK-PATTERN-AND-TEST condition])
  (let [[p & aa] condition]
    (let [[frame test]
            (if (even? (count condition))
              [(butlast condition) (last condition)]
              [condition nil])
          patt (concat (mk-typmap frame) [test])
          apid (a-indexof-pattern (template condition))]
      (if (and TRACE test) (println [:TEST-FUNCTION test]))
      (list apid patt))))

(defn enl [lst]
  "Add numbers to lists"
  (map cons (range (count lst)) lst))

(defn beta-net-plan
  "Create a plan of the beta net that will be used to its building.
   The plan describes the beta net as a list, mean while the real beta net is an array.
   The plan is the list of lists each of which represents one cell of the beta memory.
   First element of each list is an index of the beta memory, rest of each list is a content of the corresponding cell"
  ([pset]
    (enl (mapcat
           #(beta-net-plan
             (prod-name %)
             (salience %)
             (lhs %)
             (rhs %))
           pset)))
  ([pname sal lhs rhs]
    (if TRACE (println [:PRODUCTION pname]))
    (let [pts (map mk-pattern-and-test lhs)
          fir (concat (first pts) [pname])
          mid (butlast (rest pts))
          vrs (collect-vars rhs)
          las (concat (last pts)
                      (list pname sal rhs))]
      (if TRACE (println [:SALIENCE sal :RHS rhs]))
      (if (= (count lhs) 1)
        (list (cons 'ex las))
        (concat (list (cons 'e fir))
                (map #(cons 'i %) mid)
                (list (cons 'x las)) )) )))

(defn fill-ablink
  "Fill alpha-beta links table from beta net plan"
  ([ablink bplan]
    (dotimes [i (count ablink)]
      (fill-ablink ablink bplan i)))
  ([ablink bplan i]
    (let [flt (filter #(= (nth % 2) i) bplan)]
      (aset ablink i (map first flt)) )))

(defn fill-bnet [bnet bplan]
  "Fill beta net from beta net plan"
  (doseq [[i & v] bplan]
    (aset bnet i v)))

(declare reset)

(defn create-rete
  "Create RETE from a production set and reset"
  [tset pset]
  (try
    (def ANET (atom {}))
    (def ACNT (atom 0))
    (def TCNT (atom 0))
    (if TRACE (println ".... Creating TEMPLATES for Pset ...."))
    (def TEMPL
      (apply hash-map (mapcat #(list (first %) (templ-map (rest %))) tset)))
    (if TRACE (println ".... Creating ANET PLAN for Pset ...."))
    (anet-for-pset pset)
    (if TRACE (println ".... Creating BNET PLAN for Pset ...."))
    (def BPLAN (beta-net-plan pset))
    (def ABLINK (object-array @ACNT))
    (def BCNT (count BPLAN))
    (def BNET (object-array BCNT))
    (fill-bnet BNET BPLAN)
    (fill-ablink ABLINK BPLAN)
    (reset)
    (when TRACE
      (println ".... Log Files Created ....")
      (println ".... RETE Created and Reset ...."))
    [@ACNT BCNT]
    (catch :default twe
      (println twe)
      nil)))

(defn reset []
  "Reset: clear and initialize all memories"
  (def AMEM (object-array @ACNT))
  (def BMEM (object-array BCNT))
  (def CFSET (atom nil))
  (def IDFACT (atom {}))
  (def FMEM (atom {}))
  (def FCNT (atom 0))
  (def FIDS (atom {})))

(defn mk-fact [typ mp]
  "Make fact. Returns new fact id or nil if same fact exists"
  ;;(println [:MK-FACT typ mp])
  (if (nil? (mapmem-get typ mp FMEM))
    (let [fid @FCNT]
      (mapmem-put typ mp fid FMEM)
      (swap! IDFACT assoc fid [typ mp])
      (swap! FCNT inc)
      [typ mp fid])))

(defn var-vals [mp vals]
  "take values from map mp in order of list of keys"
  (map #(mp %) vals))

(declare eval-and-or)

(defn eval-and [fst rst ctx]
  ;;(println [:EVAL-AND fst rst ctx])
  (if fst
    (if (empty? rst)
      true
      (eval-and (eval-and-or (first rst) ctx) (rest rst) ctx))
    false))

(defn eval-or [fst rst ctx]
  (if fst
    true
    (if (empty? rst)
      false
      (eval-or (eval-and-or (first rst) ctx) (rest rst) ctx))))

(defn eval-and-or [x ctx]
  "Evaluate list-vector structure as and-or and vector-list structure as or-and"
  ;;(println [:EVAL-AND-OR x ctx])
  (cond
   (vector? x)
   (eval-or (eval-and-or (first x) ctx) (rest x) ctx)
   (or (list? x) (= (type x) cljs.core/LazySeq))
   (let [fst (first x)]
     (if (symbol? fst)
       (f/eval-in-ctx x ctx)
       (eval-and (eval-and-or fst ctx) (rest x) ctx)))
   true (println [:EXEPTION :X x :X-TYPE (type x)])))

(defn eval-seq [x ctx]
  "Evaluate sequense of funcalls"
  ;;(println [:EVAL-SEQ x ctx])
  (doseq [e x]
    (f/eval-in-ctx e ctx)))

(defn match [p f ctx]
  "Match two atoms against context"
  ;;(println [:MATCH p f ctx])
  (if (nil? p)
    ctx
    (if (vari? p)
      (if-let [v (ctx p)]
        (if (= f v)
          ctx)
        (assoc ctx p f))
      (if (= p f)
        ctx)) ))

(defn match-ctx [fact pattern ctx bi]
  "Match fact with pattern with respect to context"
  ;;(println [:MATCH-CTX :FACT fact :PATTERN pattern :CTX ctx :BI bi])
  (let [[ftyp fmp fid] fact
        [ptyp pmp func] pattern]
    (if (= ftyp ptyp)
      (if-let [ctx2 (loop [msk (keys (TEMPL ftyp)) xtc ctx]
                      (if (and xtc (seq msk))
                        (let [ms1 (first msk)]
                          (recur (rest msk) (match (pmp ms1) (fmp ms1) xtc)))
                        xtc))]
        (if (or (nil? func) (eval-and-or func ctx2))
          (let [ctx3 (assoc ctx2 '?fids (cons fid ('?fids ctx2)))
                fids (get @FIDS fid)]
            (if (not (some #{bi} fids))
              (reset! FIDS (assoc @FIDS fid (cons bi fids))))
            ctx3)) )) ))

(defn match-ctx-list [facts pattern ctx bi]
  "Match list of facts with pattern with respect to context"
  (map #(match-ctx % pattern ctx bi) facts))

(defn fact-id [fact]
  "Get id of fact"
  (nth fact 2))

(defn add-to-confset
  "Make from an activated production (rule) <aprod> and a list of contexts,
   that have activated this production <match-list>,
   a list of activations and concatenate them to the conflict set"
  [aprod match-list]
  ;;(println [:ADD-TO-CONFSET :APROD aprod]) ;; :MATCH-LIST match-list])
  (let [alist (map #(list aprod %) match-list)]
    (swap! CFSET concat alist)))

(defn only-old-facts [ai new-fid]
  "Take alpha memory cell content and excludes new fact if appropriate"
  ;;(println [:ONLY-OLD-FACTS ai new-fid])
  (let [ofacts (aget AMEM ai)]
    (cond
      (nil? new-fid) ofacts
      (= new-fid (fact-id (first ofacts))) (rest ofacts)
      true ofacts)))

(defn mk-match-list [ofacts pattern ctx-list bi]
  "Make match-list"
  (filter seq (mapcat #(match-ctx-list ofacts pattern % bi) ctx-list)))

(defn activate-b
  "Activate beta net cell of index <bi> with respect to a list of contexts
   already activated by a new fact with an index <new-fid>"
  [bi ctx-list new-fid]
  ;;(println [:ACTIVATE-B :BI bi :CTX-LIST ctx-list :NEW-FID new-fid])
  (let [bnode (aget BNET bi)
        [eix bal pattern & tail] bnode]
    (let [ofacts (only-old-facts bal new-fid)
          ml (mk-match-list ofacts pattern ctx-list bi)]
      ;;(println [:ML ml])
      (if (seq ml)
        (condp = eix
          'x (add-to-confset tail ml)
          'i (do
               (aset BMEM bi (concat ml (aget BMEM bi)))
               (activate-b (inc bi) ml nil))) ) )))

(defn entry-a-action [bi pattern b-mem a-mem]
  "Entry alpha activation"
  ;;(println [:ENTRY-A-ACTION :BI bi :PATTERN pattern :BMEM b-mem :AMEM a-mem])
  (let [new-fact (first a-mem)
        ctx (match-ctx new-fact pattern {} bi)]
    (aset BMEM bi (cons ctx b-mem))
    (activate-b (inc bi) (list ctx) (fact-id new-fact))))

(defn inter-a-action [bi pattern b-mem a-mem]
  "Intermediate alpha activation"
  ;;(println [:INTER-A-ACTION :BI bi :PATTERN pattern :BMEM b-mem :AMEM a-mem])
  (let [ctx-list (aget BMEM (dec bi))]
    (if (seq ctx-list)
      (let [new-fact (first a-mem)
            ml (filter seq (map #(match-ctx new-fact pattern % bi) ctx-list))]
        (when (seq ml)
          ;; remember matching context for both a-nodes and n-nodes
          (aset BMEM bi (concat ml b-mem)))
          ;; activate only a-nodes, not n-nodes
          (activate-b (inc bi) ml (fact-id new-fact))) )))

(defn exit-a-action [bi pattern tail b-mem a-mem]
  "Exit alpha activation"
  ;;(println [:EXIT-A-ACTION :BI bi :PATTERN pattern :TAIL tail :AMEM a-mem])
  (let [ctx-list (aget BMEM (dec bi))]
    (if (seq ctx-list)
      (let [ml (filter seq (map #(match-ctx (first a-mem) pattern % bi) ctx-list))]
        (when (seq ml)
          ;; remember matching context for both a-nodes and n-nodes, not for f-nodes
          (aset BMEM bi (concat ml b-mem)))
          ;; add to conflicting set only for a-nodes and f-nodes, not for n-nodes
          (add-to-confset tail ml)) ) ))

(defn enex-a-action [bi pattern tail a-mem]
  "Entry and exit alpha activation (for LHS with 1 pattern)"
  ;;(println [:ENEX-A-ACTION :PATTERN pattern :TAIL tail :AMEM a-mem])
  (if-let [ctx (match-ctx (first a-mem) pattern {} bi)]
    (add-to-confset tail (list ctx))))

(defn activate-a
  "Activate alpha net cells for index list <ais>"
  [ais]
  ;;(println [:ACTIVATE-A :AIS ais])
  (doseq [ai ais]
    (let [ablinks (aget ABLINK ai)
          bnms (map #(list % (aget BNET %) (aget BMEM %)) ablinks)
          a-mem (aget AMEM ai)]
      (doseq [[bi [eix bal pattern & tail] b-mem] bnms]
        (condp = eix
          'e (entry-a-action bi pattern b-mem a-mem)
          'i (inter-a-action bi pattern b-mem a-mem)
          'x (exit-a-action bi pattern tail b-mem a-mem)
          'ex (enex-a-action bi pattern tail a-mem))) )))

(defn fire-resolved [reso]
  "Fire resolved production"
  ;;(println [:FIRE-RESOLVED reso])
  (let [[[pn sal rhs] ctx] reso]
    (if TRACE (println [:FIRE pn :CONTEXT ctx]))
    (eval-seq rhs ctx)))

(defn f-branch [[fval tree] [pval & vls]]
  ;;(println [:F-BRANCH fval tree pval vls])
  (if (or (= '? pval) (= fval pval))
    (if (number? tree)
      true
      (some #(f-branch % vls) (seq tree)))))

(defn fact-exists? [typmap]
  "Find existing fact id for arbitrary typmap"
  ;;(println [:FACT-EXISTS typmap])
  (let [[typ mp] typmap
        fmem (@FMEM typ)
        templ (TEMPL typ)
        vls (vals (merge templ mp))]
    (if fmem
      (some #(f-branch % vls) (seq fmem)))))

(defn a-branch [[aval tree] [fval & vls]]
  ;;(println [:A-BRANCH aval tree fval vls])
  (if (or (= '? aval) (= fval aval))
    (if (number? tree)
      [tree]
      (mapcat #(a-branch % vls) (seq tree)))))

(defn a-indices [typ mp]
  "For an asserted typmap find all suitable alpha memory cells"
  ;;(println [:A-INDICES typ mp])
  (let [tree (@ANET typ)
        templ (TEMPL typ)
        vls (vals (merge templ mp))]
    (mapcat #(a-branch % vls) (seq tree))))

(defn remove-ctx-with [fid ctxlist]
  "Remove context for given fact id"
  ;;(println [:REMOVE-CTX-WITH :FID fid :CTXLIST ctxlist])
  (filter #(not (some #{fid} ('?fids %))) ctxlist))

(defn retract-b [fid bis]
  "Retract fact id from the beta memory"
  ;;(println [:RETRACT-B :FID fid :BIS bis])
  (doseq [bi bis]
    (loop [i bi]
      (aset BMEM i (remove-ctx-with fid (aget BMEM i)))
      (let [ni (inc i)]
        (if (< ni BCNT)
          (let [eix (first (aget BNET ni))]
            (if (or (= eix 'i) (= eix 'x))
              (recur ni))) )) )))

(defn frame-by-id [fid]
  "Extracts frame for fact id from facts memory"
  (let [[typ mp] (@IDFACT fid)]
    (cons typ (flatten (seq mp)))))

(defn typmapfids
  "List of facts [for given type] in form: (typ {slot-value-map} fact-id)"
  ([]
   (map #(let [[fid [typ mp]] %] [typ mp fid]) (seq @IDFACT)))
  ([typ]
   (filter #(= (first %) typ) (typmapfids))))

(defn remove-fmem [fid]
  "Remove fact from facts memory by fact id.
   Returns typmap of removed fact"
  (let [[typ mp] (@IDFACT fid)]
    (mapmem-rem typ mp FMEM)
    (swap! IDFACT dissoc fid)
    (list typ mp)))

(defn retract-fact [fid]
  "Retract fact for given fact-id by removing it from alpha, beta and fact memory,
  and also by removing from conflict set activations, containing this fact-id"
  ;;(println [:RETRACT-FACT fid])
  (if-let [frame (remove-fmem fid)]
    (let [[typ mp] frame
          ais (a-indices typ mp)]
      (if TRACE (println [:<== [typ mp] :id fid]))
      (doseq [ai ais]
        (aset AMEM ai (doall (remove #(= (fact-id %) fid) (aget AMEM ai)) ) ))
      (retract-b fid (get @FIDS fid))
      (reset! CFSET (filter #(not (some #{fid} ('?fids (second %)))) @CFSET))
      (reset! FIDS (dissoc @FIDS fid))
      frame)))

(defn ais-for-frame
  "Create fact from frame, add it to alpha memory
   and returns active list of  alpha memory cells"
  ([frame]
    ;;(println [:AIS-FOR-FRAME frame])
    (let [[typ & rst] frame
          mp (apply hash-map rst)]
      (ais-for-frame typ mp)))
  ([typ mp]
    ;;(println [:AIS-FOR-FRAME typ mp])
    (if-let [fact (mk-fact typ mp)]
      (when-let [ais (a-indices typ mp)]
        (if TRACE (println [:==> [typ mp] :id (fact-id fact)]))
        ;; fill alpha node
        (doseq [ai ais]
          (aset AMEM ai (cons fact (aget AMEM ai)) ))
        ais))))

(defn assert-frame [frame]
  "Assert frame and activate corresponding alpha nodes"
  ;;(println [:ASSERT-FRAME frame])
  (activate-a (ais-for-frame frame)))

(defn modify-fact [fid mmp]
  "Modify fact for given fact-id by retracting it and asserting,
   modified frame"
  ;;(println [:MODIFY-FACT fid mmp])
  (if-let [frame (retract-fact fid)]
    (let [[typ mp] frame
          mp2 (merge mp mmp)]
      (activate-a (ais-for-frame typ mp2)) ) ))

(defn assert-list
  "Function for assertion a list of triples or object descriptions (see comments on the function 'asser').
   For the use outside of the right hand side of rules"
  [lst]
  (activate-a (mapcat ais-for-frame lst)))

(defn fire
  "Fire!"
  ([]
    (while (not (empty? @CFSET))
      (fire 1)))
  ([n]
    (dotimes [i n]
      (if (not (empty? @CFSET))
        (let [[reso & remain] (sort-by #(- (salience (first %))) @CFSET)]
          (reset! CFSET remain)
          (fire-resolved reso)) ) )))

(defn asser
  "Function for the facts assertion that can be used in the right hand side of the rule.
   It has arbitrary number of arguments that as a whole represent a frame"
  [& args]
  (assert-frame args))

(defn not-exists
  "Function for using in left hand side to find out if some fact exists"
  [& args]
  ;;(println [:NOT-EXISTS args])
  (not (fact-exists? (mk-typmap args))))

(defn retract [fids & indices]
  "Function for the facts retraction that can be used in the right hand side of the rule.
   Retract facts for indices of patterns in left hand side of rule"
  ;;(println [:RETRACT fids indices])
  (let [fids (reverse fids)]
    (doseq [idx indices]
      (retract-fact (nth fids idx)))))

(defn modify [fids idx & svals]
  "Function for the fact modification that can be used in the right hand side of the rule.
   Modify fact for given index of pattern in left hand side of rule"
  ;;(println [:MODIFY fids idx svals])
  (let [fids (reverse fids)]
    (modify-fact (nth fids idx) (apply hash-map svals))))

(defn fact-list []
  "List of facts"
  (filter #(not= (second %) nil)
          (for [i (range @FCNT)](cons i (frame-by-id i)))))

(defn facts []
  "Prints facts"
  (let [fl (fact-list)]
    (doall (map println fl))
    (count fl)))

(defn ppr [typ]
  "Pretty prints facts of type typ or all facts when typ = :all"
  (let [all (fact-list)
        sel (if (= typ :all) all (filter #(= (second %) typ) all))]
    (doseq [fact sel]
      (println)
      (let [[[n typ] & rp] (partition-all 2 fact)]
        (println (str "Fact" n " " typ))
        (doseq [sv rp]
          (println (str "  " (first sv) " " (second sv))) )) )
    (count sel)))

(defn run-synch
 [tset pset fset]
  "Create RETE for pset and assert and fire facts from fset synchronously"
  (create-rete tset pset)
  (doseq [f fset]
    (assert-frame f)
    (fire)))

(defn run-asynch
 [tset pset fset]
  "Create RETE for pset and assert facts from fset.
   After that issue 'Fire!'"
  (create-rete tset pset)
  (assert-list fset)
  (fire))

(defn trace []
  "Begins tracing of translation and execution"
  (def TRACE true))

(defn untrace []
  "Ends tracing of translation and execution"
  (def TRACE nil))

(defn trans-lhs [lhs]
  "Translate left hand side of rule by removing statement labels and put them into map.
    Returns map of statement labels with statement indexes"
  ;;(println [:TRANS-LHS lhs])
  (loop [i 0 ss lhs mp {} nlhs []]
    (if (seq ss)
      (let [los (first ss)]
        (if (symbol? los)
          (recur (inc i) (nnext ss) (assoc mp los i) (conj nlhs (first (next ss))))
          (recur (inc i) (next ss) mp (conj nlhs los))))
      [nlhs mp])))

(defn trans-asser [x vars]
  (cons 'asser (qq (rest x) vars)))

(defn trans-retract [x mp]
  (cons 'retract
        (cons '?fids
               (map #(mp %) (rest x)) ) ))

(defn trans-modify [x vars mp]
  (cons 'modify
        (cons '?fids
                (cons (mp (first (rest x)))
                      (qq (nnext x) vars)) ) ))
(defn destruct [v]
  "Destructuring support: collect symbols inside vectors"
  (cond
   (vector? v) (mapcat destruct v)
   (= v '&) []
   true [v]))

(declare trans-rhs)

(defn trans-let [x vars mp]
  ;;(println [:TRANS-LET x vars mp])
  (let [binds (second x)
        pairs (partition 2 binds)
        vars2 (map first pairs)
        vars3 (mapcat destruct (vec vars2))
        vars4 (concat vars vars3)]
    (cons (first x)
          (cons binds (trans-rhs (nnext x) vars4 mp)))))

(defn trans-rhs [x vars mp]
  "Translate right hand side of rule by replacing in retract and modify statements
   labels of left hand side statements with their indexes using corresponding map"
  ;;(println [:TRANS-RHS x (type x) vars mp])
  (cond
   (seq? x)
     (condp = (first x)
      'asser (trans-asser x vars)
      'retract (trans-retract x mp)
      'modify (trans-modify x vars mp)
      'let (trans-let x vars mp)
      'if-let (trans-let x vars mp)
      'when-let (trans-let x vars mp)
      'loop (trans-let x vars mp)
      (map #(trans-rhs % vars mp) x))
   (vector? x) (vec (map #(trans-rhs % vars mp) x))
   true x))

(defn trans-rule [rule]
  "Translate rule by translating lhs and rhs of rule"
  ;;(println [:TRANS-RULE rule])
  (let [nam (prod-name rule)
        sal (salience rule)
        lsd (lhs rule)
        rsd (rhs rule)
        [lsd2 mp] (trans-lhs lsd)
        rsd2 (trans-rhs rsd nil mp)]
    (concat [nam] [sal] lsd2 ['=>] rsd2)))

(defn run-with
  [modes temps rules facts]
  ;;(println [:RUN-WITH modes temps rules facts])
  (if (= modes "step:asynch")
    (let [trules (map trans-rule rules)]
      (trace)
      (create-rete temps trules)
      (assert-list facts)
      (fire 1))
    (let [[m1 m2] (seq (.split modes ":"))
          trules (map trans-rule rules)]
      (if(condp = m1
           "trace" (do (trace) true)
           "run" (do (untrace) true)
           (do (println (str "Wrong mode: " m1)) nil))
        (condp = m2
          "synch" (run-synch temps trules facts)
          "asynch" (run-asynch temps trules facts)
          (println (str "Wrong mode: " m2))) ) )))

(defn frames-of-type [typ]
  "Extracts frames for type of fact from facts memory"
  (let [tmfs (typmapfids typ)]
    (map #(let [[typ mp fid] %] (cons typ (flatten (seq mp)))) tmfs)))

(run-with "run:asynch" temps/TEMPLATES rules/RULES facts/FACTS)
