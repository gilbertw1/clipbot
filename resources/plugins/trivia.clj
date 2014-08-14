(ns clipbot.plugins.trivia
  (:require [clipbot.plugin :as plugin]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(def trivia-dir (-> "plugin_resources/trivia" io/resource))
(def questions (atom []))
(def game (atom {:curr nil :players {}}))

(defn convert-question [line]
  (let [[question answer] (str/split line #"\*")]
    {:q question :a answer}))

(defn load-question-file [file]
  (with-open [rdr (io/reader file)]
    (doall (map convert-question (line-seq rdr)))))

(defn load-trivia-questions []
  (vec (flatten (map load-question-file (->> trivia-dir io/file file-seq (filter #(not (.isDirectory %))))))))

(reset! questions (load-trivia-questions))

(defn new-question []
  (rand-nth @questions))

(defn has-answer? [msg]
  (when-let [answer (-> @game :curr :a)]
    (re-seq (->> answer (str "(?i)") re-pattern) msg)))

(defn inc-score [g user]
  (if (-> g :players (get user))
    (update-in g [:players user] inc)
    (assoc-in g [:players user] 1)))

(defn increment-user-score [user responder]
  (swap! game inc-score user)
  (when responder
    (responder (str "Correct Answer! -- " user " has scored 1 point!"))))

(defn set-new-game-question [responder]
  (swap! game assoc :curr (new-question))
  (when responder
    (responder "**** New Question *****")
    (responder (-> @game :curr :q))))

(defn clear-game []
  (reset! game {:curr (new-question) :players {}}))

(defn start-new-game [responder]
  (clear-game)
  (responder "New Trivia Game Started!")
  (set-new-game-question responder))

(defn check-and-handle-answer [responder user msg]
  (when (has-answer? msg)
    (increment-user-score user responder)
    (set-new-game-question responder)))

(defn respond-with-scores [responder]
  (responder "Trivia Scores\n-------------")
  (doseq [[user score] (:players @game)]
    (responder (str user ": " score))))

(defn extract-command [msg]
  (->> msg (re-seq #"\$trivia\s+(\w+)") first second))

(defn handle-command [cmd responder]
  (condp = (str/lower-case cmd)
    "new" (start-new-game responder)
    "scores" (respond-with-scores responder)
    "skip" (set-new-game-question responder)))

(plugin/register-plugin
  {:id "trivia"
   :handlers [{:regex #"\$trivia\s+(\w+)"
               :function (fn [responder user msg]
                           (handle-command (extract-command msg) responder))}
              {:regex #".*"
               :function (fn [responder user msg]
                           (check-and-handle-answer responder user msg))}]})