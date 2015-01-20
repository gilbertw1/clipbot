(ns clipbot.plugins.weather
  (:require [clipbot.plugin :as plugin]
            [clj-http.client :as http]))

(def fing-weather-url "http://thefuckingweather.com/")

(defn match [s rx]
  (->> s (re-seq rx) first second))

(defn get-weather [place]
  (let [html (-> fing-weather-url (http/get {:query-params {:where place} :headers {:User-Agent "Mozilla/5.0"}}) :body)
        temp (or (match html #"<span class=\"temperature\" tempf=\"\d*\">(.*?)<") "")
        remark (or (match html #"<p class=\"remark\">(.*)<") "remark not found")
        flavor (or (match html #"<p class=\"flavor\">(.*)<") "flavor not found")]
    (when temp
      (str temp " degrees -- " remark "  *" flavor "*"))))

(plugin/register-plugin
  {:id "weather"
   :regex #"\$weather\s+(.*)"
   :function (fn [responder user msg]
               (responder "Let me get that for ya")
               (-> msg (match #"\$weather\s+(.*)") get-weather responder))})
