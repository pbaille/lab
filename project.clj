(defproject libtest "0.1.0-SNAPSHOT"
            :description "FIXME: write description"
            :url "http://example.com/FIXME"
            :license {:name "Eclipse Public License"
                      :url "http://www.eclipse.org/legal/epl-v10.html"}
            :dependencies [[org.clojure/clojure "1.8.0"]
                           [org.clojure/clojurescript "1.9.293"]
                           [org.clojure/core.async "0.2.395"]
                           [figwheel-sidecar "0.5.0"]
                           [rum "0.10.8"]
                           [cljs-http "0.1.39"]

                           ;; libs to test
                           [defun "0.3.0-RC1"]
                           [potemkin "0.4.3"]
                           [prismatic/plumbing "0.5.3"]
                           [com.rpl/specter "0.13.2"]
                           [better-cond "1.0.1"]
                           [traversy "0.4.0"]
                           [funcool/lentes "1.2.0"]
                           [net.cgrand/xforms "0.8.2"]
                           [im.chit/purnam "0.5.2"]

                           [funcool/promesa "1.7.0"]
                           [funcool/httpurr "0.6.2"]
                           [funcool/beicon "2.8.0"]
                           [funcool/bide "1.4.0"]
                           [funcool/potok "1.2.0"]
                           [funcool/struct "1.0.0"]
                           [funcool/wydra "0.1.0-SNAPSHOT"]
                           [manifold "0.1.5"]
                           [aleph "0.4.1"]
                           [automat "0.2.0"]

                           [fungp "0.3.2"]
                           [org.clojure/core.match "0.3.0-alpha4"]
                           [reagi "0.10.1"]

                           [thi.ng/fabric "0.0.388"]
                           [thi.ng/domus "0.3.0-SNAPSHOT"]

                           [thi.ng/geom "0.0.908"]
                           [thi.ng/color "1.2.0"]
                           [rm-hull/monet "0.3.0"]
                           [cljsjs/d3 "4.3.0-2"]

                           [chronoid "0.1.1"]
                           [mantra "0.6.0"]
                           [cljs-bach "0.2.0"]]

            :plugins [[lein-cljsbuild "1.1.0"]]
            :source-paths ["src" "script"]
            :cljsbuild {:builds [{:id           "min"
                                  :source-paths ["src"]
                                  :compiler     {:main          'libtest.core
                                                 :asset-path    "js/out"
                                                 :optimizations :advanced
                                                 :output-to     "resources/public/js/out/main.min.js"
                                                 :output-dir    "resources/public/js/out"}}]})
