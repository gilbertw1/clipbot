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
  (map slurp (->> (io/resource "plugins") io/file file-seq (filter #(not (.isDirectory %))))))

(defn load-plugins []
  (doseq [plugin (read-plugin-files)]
    (load-string plugin)))

(defroutes app-routes
  (GET "/" [] "I am the clipbot, goo goo g'joob")
  (route/not-found "not found"))

(defn start [conf-file]
  (load-plugins)
  (let [{bot-confs :bots} (read-conf conf-file)
        bots (map #(bot/create % @plugin/plugins) bot-confs)
        connected-bots (doall (map chat/connect-bot bots))]
    (httpkit/run-server app-routes {:port 8080})))

(defn -main [& [conf-file & args]]
  (def app (start conf-file)))