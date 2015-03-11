(ns dev
  (:require [clipbot.core :as clipbot]
            [disposables.core :refer [verbose-dispose]]
            [clojure.tools.namespace.repl :as tns]))

(defonce app (atom nil))

(defn stop-app []
  (when @app (println (verbose-dispose @app)))
  (reset! app nil))

(defn start-app []
  (if-not @app
    (reset! app (clipbot/start))
    (println "App already started, use reload-app instead")))

(defn reload-app []
  (stop-app)
  (tns/refresh :after 'dev/start-app))
