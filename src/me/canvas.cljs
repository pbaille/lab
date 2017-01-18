(ns me.canvas
  (:require-macros [cljs.core.async.macros :refer [go-loop go]]
                   [purnam.core :refer [? ?> ! !> f.n def.n do.n
                                        obj arr def* do*n def*n f*n]])
  (:require [monet.canvas :as m]
            [rum.core :as r]
            [me.chans-mixin :as cm]
            [thi.ng.domus.core :as dom]
            [cljs.core.async :as async :refer [chan >! <!]]
            [me.utils :as u]))

(enable-console-print!)

(defn clear! [canvas]
  (let [ctx (m/get-context canvas "2d")
        w (.-width canvas)
        h (.-height canvas)]
    (m/clear-rect ctx {:x 0 :y 0 :w w :h h})))

(def ctx-actions
  {:begin-path m/begin-path
   :close-path m/close-path
   :save m/save
   :restore m/restore
   :rotate m/rotate
   :scale m/scale
   :translate m/translate
   :transform m/transform
   :fill m/fill
   :stroke (fn [ctx] (println "stroke") (m/stroke ctx))
   :clip m/clip
   :rect m/rect
   :clear-rect m/clear-rect
   :stroke-rect m/stroke-rect
   :fill-rect m/fill-rect
   :arc m/arc
   :ellipse m/ellipse
   :circle m/circle
   :text m/text
   :font-style m/font-style
   :fill-style m/fill-style
   :stroke-style m/stroke-style
   :stroke-width m/stroke-width
   :stroke-cap m/stroke-cap
   :stroke-join m/stroke-join
   :move-to (fn [ctx [x y]] (m/move-to ctx x y))
   :line-to (fn [ctx [x y]] (m/line-to ctx x y))
   :alpha m/alpha
   :composition-operation m/composition-operation
   :text-align m/text-align
   :text-baseline m/text-baseline
   :draw-image m/draw-image
   :quadratic-curve-to m/quadratic-curve-to
   :bezier-curve-to m/bezier-curve-to
   :rounded-rect m/rounded-rect})

(defn do-ctx-cmd [ctx [v & args]]
  (apply (get ctx-actions v) ctx args))

(defn redraw [s])

(defn init-state! [s]
  (swap! (:state s)
         assoc
         :ctx-cmds []
         :cmds [])
  s)

(defn init-canvas! [s]
  (let [c (dom/query (r/dom-node s) "canvas")
        ctx (.getContext c "2d")]
    (set! (.-strokeStyle ctx) "rgba(0,0,0,.4)")
    (set! (.-lineWidth ctx) 1)
    (swap! (:state s)
           assoc
           :ctx ctx
           :canvas c)
    s))

(declare actions)

(defn act1 [s [k & args]]
  (apply (get actions k (fn [s & _] s)) s args))

(defn act [s & xs]
  (reduce act1 s xs))

(def act* (partial apply act))

(def actions
  {:ctx (fn [s cmd]
          (do-ctx-cmd (:ctx s) cmd)
          (update s :ctx-cmds conj cmd))

   :clear-cmds (fn [s]
                 (assoc s :cmds []))

   :redraw (fn [s]
             (clear! (:canvas s))
             (doseq [c (:ctx-cmds s)]
               (do-ctx-cmd (:ctx s) c))
             s)

   :undo (fn [s]
           (act (update s :ctx-cmds (comp vec butlast))
                [:redraw]))

   :clear (fn [s]
            (clear! (:canvas s))
            s)

   :size (fn [{c :canvas :as s} w h]
           (! c.width w)
           (! c.height h)
           s)

   :restore (fn [s]
              (if-let [bu (:ctx-backup s)]
                (dissoc (assoc s :ctx bu)
                        :ctx-backup)
                s))

   :save-ctx (fn [s]
               (assoc s :ctx-backup (js/Object.assign #js {} (:ctx s))))})

(defn dispatch [s & cmds]
  (apply (-> s :state deref :dispatch) cmds))

(r/defcs canvas <
  (cm/channelled
    {:actions actions
     :notifications {}
     ;:before #(swap! %1 update :cmds into %2)
     :after #(swap! %1 update :cmds into %2)
     })
  {:init init-state!
   :should-update (constantly false)
   :did-mount init-canvas!}
  [s opts]
  [:.canvas-wrap
   [:button {:on-click (fn [_]
                         (dispatch s
                                   [:ctx [:circle {:x (rand-int 200) :y (rand-int 200) :r (rand-int 200)}]]
                                   [:ctx [:stroke]]))}
    "add circle"]
   [:button {:on-click (fn [_]
                         (dispatch s [:redraw]))}
    "redraw"]
   [:button {:on-click (fn [_]
                         (dispatch s [:undo]))}
    "undo"]
   [:button {:on-click (fn [_]
                         (dispatch s [:save]))}
    "save"]
   [:button {:on-click (fn [_]
                         (dispatch s [:restore]))}
    "restore"]
   [:canvas]])

(let [in (chan)
      out (chan)
      c (canvas {:in-chan in
                 :out-chan out})]
  (r/mount c (dom/query nil "#app"))
  (go (>! in [[:size 500 500]
              [:ctx [:circle {:x 100 :y 100 :r 20}]]
              [:ctx [:stroke]]]))

  c)