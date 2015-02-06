(ns clipbot.core
  (:gen-class)
  (:require
   [clipbot.chat :as chat]
   [clipbot.bot :as bot]
   [clipbot.plugin :as plugin]
   [clojure.java.io :as io]
   [cheshire.core :as json])
  (:import [rx.subjects PublishSubject]))

(def resource-conf (-> "config.json" io/resource))

(defn read-conf [file]
  (json/parse-string (slurp (or file resource-conf)) true))

(defn start
  ([] (start nil))
  ([conf-file]
   (start conf-file (PublishSubject/create)))
  ([conf-file subject]
   (let [conf (read-conf conf-file)
         {bot-configs :bots} conf
         plugins (plugin/load-plugins)]
     (chat/init-chat bot-configs plugins subject))))

(defn -main [& [conf-file & args]]
  (start conf-file))
