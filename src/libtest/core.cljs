(ns libtest.core)

(enable-console-print!)

(defonce state
         (atom {:text "Hello Curly!"}))

(defn main []
      [:div (:text @state)])
