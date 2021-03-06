(ns me.utils
  (:require [rum.core :as rum]
            [cljs.core.match :refer-macros [match]]))

;; generics ------------------------------------------------

(defn $1 [sel]
  (js/document.querySelector sel))

(defn log [& xs]
  (doseq [x xs]
    (js/console.log x)))

(defn tval [x] (.. x -target -value))

(defn now [] (.getTime (js/Date.)))

(defn get-mouse-signal [& [el]]
  (let [mouse-position (atom {:x 0 :y 0})]
    (aset (or el js/document)
          "onmousemove"
          (fn [e] (js/console.log e)
            (reset! mouse-position
                    {:y (.-offsetY e)
                     :x (.-offsetX e)})))
    mouse-position))

(def p println)

;; colls -----------------------------------------------------

(defn index-of [coll value]
  (some (fn [[idx item]] (when (= value item) idx))
        (map-indexed vector coll)))

(defn indexes-of [coll value]
  (keep
    (fn [[idx item]] (when (= value item) idx))
    (map-indexed vector coll)))

(defn rem-idx [v idx]
  (vec (concat (subvec v 0 idx) (subvec v (inc idx)))))

(defn vinsert [v idx & vls]
  (vec (concat (subvec v 0 idx) vls (subvec v idx))))

(letfn [(merge-in* [a b]
          (if (map? a)
            (merge-with merge-in* a b)
            b))]
  (defn merge-in
    "Merge multiple nested maps."
    [& args]
    (reduce merge-in* nil args)))

;; numerics --------------------------------------------------

(defn with-precision [p n]
  (js/parseFloat (.toFixed n p)))

(defn bounder [min max]
  (fn [x]
    (cond
      (< x min) min
      (> x max) max
      :else x)))

;from overtone
(defn scale-range
  "Scales a given input value within the specified input range to a
  corresponding value in the specified output range using the formula:

           (out-max - out-min) (x - in-min)
   f (x) = --------------------------------  + out-min
                    in-max - in-min
  "
  [x in-min in-max out-min out-max]
  (+ (/ (* (- out-max out-min) (- x in-min))
        (- in-max in-min))
     out-min))

(defn interpolator [pts]
  (let [pts (sort-by first (if (:x (first pts)) (map (juxt :x :y) pts) pts))]
    (fn [x]
      (cond
        (<= x (ffirst pts)) (-> pts first second)
        (>= x (-> pts last first)) (-> pts last second)
        :else
        (let [[[x1 y1] [x2 y2]] (first (filter (fn [[[x1 _] [x2 _]]] (<= x1 x x2)) (partition 2 1 pts)))]
          (scale-range x x1 x2 y1 y2))))))

(defn interp-seq [interpolator start end step]
  (map (fn [x] [x (interpolator x)]) (range start (+ end step) step)))



;; reagent ---------------------------------------------------------

(defn component
  [{:keys [render] :as spec}]
  {:pre [(ifn? render)]}
  (let [name (or (:name spec) (str (gensym "rum-")))
        mixins (or (:mixins spec) [])
        spec (dissoc spec :name :mixins :render)
        render' (fn [state]
                  [(apply render state (:rum/args state)) state])
        mixins (conj mixins spec)]
    (rum/build-ctor render' mixins name)))

(defn rwrap [build deps-map]
  (let [this (atom (build))
        deps (keys deps-map)
        propagating (atom nil)]
    (add-watch this
               :build
               (fn [_ _ _ n]
                 (when-not @propagating
                   (reset! propagating true)
                   (doseq [[dep upd] deps-map]
                     (swap! dep (or upd (fn [x _] x)) n))
                   (reset! propagating false))))
    (doseq [r deps]
      (add-watch r
                 (keyword (gensym))
                 (fn [_ _ _ _]
                   (when-not @propagating
                     (reset! propagating true)
                     (reset! this (build))
                     (reset! propagating false)))))
    this))

(defn rwrap2
  ([build reactions]

   (let [deps (map first reactions)
         this (atom (apply build (map deref deps)))
         propagating (atom nil)]
     (add-watch this
                :build
                (fn [_ _ _ n]
                  (when-not @propagating
                    (reset! propagating true)
                    (doseq [[dep upd] reactions]
                      (swap! dep (or upd (fn [x _] x)) n))
                    (reset! propagating false))))
     (doseq [r deps]
       (add-watch r
                  (keyword (gensym))
                  (fn [_ _ _ _]
                    (when-not @propagating
                      (reset! propagating true)
                      (reset! this (apply build (map deref deps)))
                      (reset! propagating false)))))
     this))
  ([build r1 & rs]
   (rwrap2 build (cons r1 rs))))

(comment

  (let [atom? #(implements? IDeref %)
        reaction (s/tuple atom? fn?)]

    (s/fdef rwrap2
            :args (s/cat :build fn?
                         :reactions
                         (s/or :reaction-vec (s/coll-of reaction)
                               :reaction reaction)
                         :variadic-reactions (s/* reaction))
            :ret atom?))

  (stest/instrument `rwrap2)

  (add 1 2.1)
  (let [dep1 (atom 10)
        dep2 (atom 0)
        this (rwrap2
               (fn [d1 d2] {:a d1 :b d2})
               [[dep1 (fn [_ v] (:a v))]
                [dep2 (fn [_ v] (:b v))]])]

    (p "<> (swap! this update :a inc) <>")
    (swap! this update :a inc)

    (p "<> (swap! dep1 inc) <>")
    (swap! dep1 inc)

    (p "<> (swap! dep2 dec) <>")
    (swap! dep2 dec)

    (assert (and
              (= @dep1 12)
              (= @dep2 -1)
              (= @this {:a 12 :b -1})))

    nil))
