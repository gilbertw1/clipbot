(ns clipbot.core
  (:gen-class)
  (:use compojure.core)
  (:require [clipbot.chat :as chat]
            [clojure.java.io :as io]
            [cheshire.core :as json]
            [org.httpkit.server :as httpkit]
            [compojure.route :as route]))

(def bots (atom []))
(def resource-conf (-> "config.json" io/resource))

(defn read-conf [file]
  (json/parse-string (slurp (or file resource-conf)) true))

(defroutes app-routes
  (GET "/" [] "I am the clipbot, goo goo g'joob")
  (route/not-found "not found"))

(defn start [conf-file]
  (let [{bots :bots} (read-conf conf-file)
        chat-conn (chat/connect (:connection (first bots)) (fn [resp msg] (println "Received Message: " msg)))]
    (httpkit/run-server app-routes {:port 8080})))

(defn -main [& [conf-file & args]]
  (def app (start conf-file)))