(ns clipbot.plugins.pug
  (:require [clipbot.plugin :as plugin]
            [cheshire.core :as json]
            [clj-http.client :as http]))

(def rand-pug-url "http://pugme.herokuapp.com/random")
(def pug-bomb-url "http://pugme.herokuapp.com/bomb")

(defn show-pug [responder]
  (let [res (-> rand-pug-url (http/get) :body (json/parse-string true))
        pug (:pug res)]
    (responder pug)))

(defn pug-bomb [responder cnt]  
  (let [count (if (> cnt 15) 15 cnt)
        res (-> (str pug-bomb-url "?count=" count) (http/get) :body (json/parse-string true))
        pugs (:pugs res)]
    (doseq [pug pugs] (responder pug))))

(defn extract-count [msg]
  (Integer/parseInt (->> msg (re-seq #"\$pugbomb\s(\d+)") first second)))

(plugin/register-plugin
  {:id "pug"
   :handlers [{:regex #"\$pugme"
               :function (fn [responder user msg]
                           (show-pug responder))}
              {:regex #"\$pugbomb\s(\d+)"
               :function (fn [responder user msg]
                           (pug-bomb responder (extract-count msg)))}]})