(ns clipbot.plugin
  (:gen-class))

(def plugins (atom {}))

(defrecord Plugin [id handlers])

(defrecord PluginHandler [regex function])

(defn register-plugin [plugin]
  (println "Registering Plugin: " plugin)
  (swap! plugins assoc  (:id plugin) plugin))