(ns clipbot.plugin
  (:gen-class)
  (:require [clojure.java.io :as io]))

(defonce plugins (atom {}))

(defn read-plugin-files []
  (map slurp (->> "plugins/clipbot/plugins" io/resource io/file file-seq (filter #(not (.isDirectory %))))))

(defn load-plugins []
  (doseq [plugin (read-plugin-files)]
    (load-string plugin))
  @plugins)

(defn register-plugin [plugin]
  (println "Registering Plugin: " plugin)
  (swap! plugins assoc (:id plugin) plugin))
