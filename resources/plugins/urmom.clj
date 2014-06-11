(ns clipbot.plugins.urmom
  (:require [clipbot.plugin :as plugin]
            [clojure.string :as str]))

(defn extract-ing-word [msg]
  (->> msg (re-seq #"(\w+ing)") first second))

(defn extract-statement-words [msg]
  (let [ing-word (extract-ing-word msg)
        ing-string (subs msg (.indexOf msg ing-word))]
    (str/split ing-string #"\s")))

(defn tell-ur-mom [user words]
  (str/replace (str user "'s mom is " (str/join " " words)) #"[^A-Za-z1-9\s']" ""))

(defn should-run [words msg]
  (and
    (> (rand-int 10) 7)
    (< (count words) 10)
    (not (.contains msg "mom"))))

(plugin/register-plugin
  {:id "urmom"
   :regex #"\w+ing"
   :function (fn [responder user msg]
               (let [statement-words (extract-statement-words msg)]
                 (when (should-run statement-words msg)
                   (responder (tell-ur-mom user statement-words)))))})