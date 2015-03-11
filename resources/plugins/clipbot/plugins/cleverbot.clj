(ns clipbot.plugins.cleverbot
  (:require [clipbot.plugin :as plugin]
            [clj-http.client :as http]
            [clojure.string :as string]
            [pandect.core :as pandect]))

(def default_params
  (hash-map
    :start 'y,
    :icognoid 'wsf,
    :fno 0,
    :sub 'Say,
    :islearning 1,
    :cleanslate 'false))

(def cleverbot_url "http://www.cleverbot.com/webservicemin")

(defn build_query [params]
     (string/join "&" (for [[k v] params] (str (name k) "=" v))))

(defn chat_query [msg]
  (string/join " " (drop 1 (string/split msg #"\s+"))))

(defn query_cleverbot [query]
  (let
    [base_params (build_query (assoc default_params :stimulus query))
     icognocheck (pandect/md5 (subs base_params 9 35))
     query-params (str base_params "&icognocheck=" icognocheck)]
    (first
      (re-find #"(.*)\r"
        (get  
          (http/with-middleware (remove #(= % clj-http.cookies/wrap-cookies) http/default-middleware) 
            (http/post cleverbot_url {:body query-params 
                                      :client-params {"http.useragent" "clj-http"}})) :body)))))

(plugin/register-plugin
  {:id "cleverbot"
   :regex #"\@ClippardBotsworth\s+(.*)"
   :function (fn [responder user msg]
                (-> msg
                    chat_query
                    query_cleverbot
                    responder))})