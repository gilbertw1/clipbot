(ns clipbot.core
  (:gen-class)
  (:use compojure.core)
  (:require [clipbot.chat :as chat]
            [clipbot.bot :as bot]
            [clipbot.plugin :as plugin]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [cheshire.core :as json]
            [org.httpkit.server :as httpkit]
            [compojure.route :as route]
            [ring.middleware.basic-authentication :refer [wrap-basic-authentication]]
            [compojure.handler :refer [site]]
            [hiccup.core :refer [html]])
  (:import [rx.subjects PublishSubject])
  )

(def resource-conf (-> "config.json" io/resource))

(def chat-bots (atom []))

(defn admin-authenticated? [name pass]
  (and (= name "admin")
       (= pass "adminone2three4")))

(defn read-conf [file]
  (json/parse-string (slurp (or file resource-conf)) true))

(defn read-plugin-files []
  (map slurp (->> "plugins/clipbot/plugins" io/resource io/file file-seq (filter #(not (.isDirectory %))))))

(defn load-plugins []
  (doseq [plugin (read-plugin-files)]
    (load-string plugin)))

(defn admin-bot-link [bot]
  [:li [:a {:href (str "/admin/" (:id bot))} (:id bot)]])

(defn list-bots-view []
  (html
    [:html
      [:body
        (map admin-bot-link @chat-bots)]]))

(defn find-bot [botid]
  (->> @chat-bots (filter #(= (:id %) botid)) first))

(defn room-checkbox [[id _]]
  [:input.room-box {:type "checkbox" :name id :value id} id [:br]])

(defn js-send [botid]
  (str 
    "var send = function() {
      var msg = $('textarea.msg').val();
      var checkedValues = $('input:checkbox:checked').map(function() { return this.value; }).get().join(',');
      window.location.href = ('/admin/send/" botid "?msg=' + encodeURIComponent(msg) + '&rooms=' + checkedValues);
    };"))

(defn chat-view [bot]
  (html
    [:html
      [:head
        [:script {:src "//ajax.googleapis.com/ajax/libs/jquery/2.1.1/jquery.min.js"}]
        [:script {:type "text/javascript"} (js-send (:id bot))]]
      [:body
        [:textarea.msg {:row "10" :cols "70"}]
        (map #(room-checkbox %) (-> bot :conn (chat/list-channels)))
        [:button {:type "button" :onclick "send();"} "Send!"]]]))

(defn prt [obj]
  (println obj)
  obj)

(defn find-room [bot room]
  (->> bot :conn (chat/list-channels) (filter #(= (first %) room)) first second first prt))

(defn send-in-room [bot room msg]
  (when-let [room (find-room bot room)]
    (.sendMessage (:conn room) msg)))

(defn send-chat-view [bot rooms msg]
  (doseq [room rooms]
    (send-in-room bot room msg))
  (html
    [:html
      [:body
        [:h1 "Message Sent!"]]]))

(defroutes app-routes
  (GET "/" [] "I am the clipbot, goo goo g'joob")
  (GET "/admin" [] (list-bots-view))
  (GET "/admin/:botid" [botid] (chat-view (find-bot botid)))
  (GET "/admin/send/:botid" [botid msg rooms] (send-chat-view (find-bot botid) (str/split rooms #",") (java.net.URLDecoder/decode msg)))
  (route/not-found "not found"))

(defn start [subject conf]
  (load-plugins)
  (let [{server-port :server-port} conf
        {bot-confs :bots} conf
        bots (map #(bot/create % subject @plugin/plugins) bot-confs)
        connected-bots (doall (map chat/connect-bot bots))]
    (println (str "Running on port: " server-port))
    (reset! chat-bots connected-bots)
    (httpkit/run-server (site app-routes) {:port server-port})))

(defn -main [& [conf-file & args]]
  (let [conf (read-conf conf-file)
        subject (PublishSubject/create)]
    (def app (start subject conf))))
