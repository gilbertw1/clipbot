(ns clipbot.plugins.image
  (:require [clipbot.plugin :as plugin]
            [cheshire.core :as json]
            [clj-http.client :as http]))

(def image-search-url "http://ajax.googleapis.com/ajax/services/search/images")

(defn google-image [query]
  (let [res (-> image-search-url 
                (http/get {:query-params {:v "1.0" :rsz "8" :q query}}) 
                :body 
                (json/parse-string true))]
    (-> res :responseData :results rand-nth :unescapedUrl)))

(defn extract-query [msg]
  (->> msg (re-seq #"\$imageme\s+(.*)") first second))

(plugin/register-plugin
  {:id "image"
   :regex #"\$imageme\s+(.*)"
   :function (fn [responder user msg]
               (-> msg extract-query google-image responder))})