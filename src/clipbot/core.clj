(ns clipbot.core
  (:gen-class)
  (:use compojure.core)
  (:require [clipbot.chat :as chat]
            [clipbot.bot :as bot]
            [clipbot.plugin :as plugin]
            [clojure.java.io :as io]
            [cheshire.core :as json]
            [org.httpkit.server :as httpkit]
            [compojure.route :as route]))

(def resource-conf (-> "config.json" io/resource))

(defn read-conf [file]
  (json/parse-string (slurp (or file resource-conf)) true))

(defn read-plugin-files []
  (map slurp (->> "plugins" io/resource io/file file-seq (filter #(not (.isDirectory %))))))

(defn load-plugins []
  (doseq [plugin (read-plugin-files)]
    (load-string plugin)))

(defroutes app-routes
  (GET "/" [] "I am the clipbot, goo goo g'joob")
  (route/not-found "not found"))

(defn start [conf]
  (load-plugins)
  (let [{server-port :server-port} conf
        {bot-confs :bots} conf
        bots (map #(bot/create % @plugin/plugins) bot-confs)
        connected-bots (doall (map chat/connect-bot bots))]
    (println (str "Running on port: " server-port))
    (httpkit/run-server app-routes {:port server-port})))

(defn -main [& [conf-file & args]]
  (let [conf (read-conf conf-file)]
    (def app (start conf))))
