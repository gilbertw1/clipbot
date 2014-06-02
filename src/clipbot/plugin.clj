(ns clipbot.plugin
  (:gen-class))

(def plugins (atom {}))

(defn register-plugin [plugin]
  (println "Registering Plugin: " plugin)
  (swap! plugins assoc  (:id plugin) plugin))