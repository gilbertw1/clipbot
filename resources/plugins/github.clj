(ns clipbot.plugins.github
  (:require [clipbot.plugin :as plugin]
            [tentacles.repos :as repos]
            [tentacles.events :as events]
            [clojure.string :as string]))


(def gh-user-feed-rx #"^\s*gh\suser-feed\s(.*)")

(defn convert-event [{:keys [type actor repo]}]
  (str "type: " type ", user: " (:login actor) ", repo: " (:name repo)))

(plugin/register-plugin
  {:id "github"
   :regex #"\$github\s(\w+)"
   :function (fn [responder user msg]
               (println "Github info for: " msg)
               (let [gh-user (second (string/split msg #"\s"))]
                 (println "Github info for user: " gh-user)
                 (dorun
                   (map (comp responder convert-event)
                        (take 5 (drop 1 (events/performed-events gh-user {:per-page 5})))))))})