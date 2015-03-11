(ns clipbot.plugins.github
  (:require [clipbot.plugin :as plugin]
            [tentacles.repos :as repos]
            [tentacles.events :as events]
            [clojure.string :as string]))

(defn convert-event [{:keys [type actor repo]}]
  (str "type: " type ", user: " (:login actor) ", repo: " (:name repo)))

(plugin/register-plugin
  {:id "github"
   :regex #"\$github\s(\w+)"
   :function (fn [responder user msg]
               (let [gh-user (second (string/split msg #"\s"))]
                 (dorun
                   (map (comp responder convert-event)
                        (take 5 (drop 1 (events/performed-events gh-user {:per-page 5})))))))})