(ns clipbot.plugins.beer30
  (:require [clipbot.plugin :as plugin]
            [cheshire.core :as json]
            [clj-http.client :as client]))

(def beer30-status-url "https://beer30.sparcedge.com/status")

(defn get-beer30-status []
  (try
    (let [res (client/get beer30-status-url)
          resjs (json/parse-string (:body res) true)
          status (:statusType resjs)
          reason (:reason resjs)]
      (println "Beer30 Status: " status)
      (println "Beer30 Reason: " reason)
      (cond
        (= status "CAUTION") (str "beer30 is yellow :/ (" reason ")")
        (= status "STOP") (str "beer30 is red :( (" reason ")")
        (= status "GO") (str "beer30 is green :) (" reason ")")
        :else "unknown beer30 status"))
    (catch Exception e "beer 30 is down :(")))

(plugin/register-plugin
  {:id "beer30"
   :regex #"\$beer30"
   :function (fn [responder user msg]
               (responder (get-beer30-status)))})