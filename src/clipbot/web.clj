(ns clipbot.web)
;; (ns clipbot.web
;;   (:require
;;    [clojure.string :as str]
;;    [compojure.core :refer [routes GET]]
;;    [compojure.handler :refer [site]]
;;    [compojure.route :as route]
;;    [hiccup.core :refer [html]]
;;    [org.httpkit.server :as httpkit]
;;    [ring.middleware.basic-authentication :refer [wrap-basic-authentication]]
;;    [rx.lang.clojure.interop :refer [action*]]

;;    [clipbot.chat :as chat])
;;   (:import [rx.subscriptions Subscriptions]))

;; (defn js-send [botid]
;;   (str
;;     "var send = function() {
;;       var msg = $('textarea.msg').val();
;;       var checkedValues = $('input:checkbox:checked').map(function() { return this.value; }).get().join(',');
;;       window.location.href = ('/admin/send/" botid "?msg=' + encodeURIComponent(msg) + '&rooms=' + checkedValues);
;;     };"))

;; (defn admin-bot-link [bot]
;;   [:li [:a {:href (str "/admin/" (:id bot))} (:id bot)]])

;; (defn list-bots-view [bots]
;;   (html
;;     [:html
;;       [:body
;;         (map admin-bot-link bots)]]))

;; (defn room-checkbox [[id _]]
;;   [:input.room-box {:type "checkbox" :name id :value id} id [:br]])

;; (defn chat-view [bot]
;;   (html
;;     [:html
;;       [:head
;;         [:script {:src "//ajax.googleapis.com/ajax/libs/jquery/2.1.1/jquery.min.js"}]
;;         [:script {:type "text/javascript"} (js-send (:id bot))]]
;;       [:body
;;         [:textarea.msg {:row "10" :cols "70"}]
;;         (map #(room-checkbox %) (-> bot :conn (chat/list-channels)))
;;         [:button {:type "button" :onclick "send();"} "Send!"]]]))

;; (defn find-bot [bots botid]
;;   (->> bots (filter #(= (:id %) botid)) first))

;; (defn prt [obj]
;;   (println obj)
;;   obj)

;; (defn find-room [bot room]
;;   (->> bot :conn (chat/list-channels) (filter #(= (first %) room)) first second first prt))

;; (defn send-in-room [bot room msg]
;;   (when-let [room (find-room bot room)]
;;     (.sendMessage (:conn room) msg)))

;; (defn send-chat-view [bot rooms msg]
;;   (doseq [room rooms]
;;     (send-in-room bot room msg))
;;   (html
;;     [:html
;;       [:body
;;         [:h1 "Message Sent!"]]]))

;; (defn app-routes [bots]
;;   (routes
;;    (GET "/" [] "I am the clipbot, goo goo g'joob")
;;    (GET "/admin" []
;;         (list-bots-view bots))
;;    (GET "/admin/:botid" [botid]
;;         (chat-view (find-bot bots botid)))
;;    (GET "/admin/send/:botid" [botid msg rooms]
;;         (send-chat-view (find-bot botid) (str/split rooms #",") (java.net.URLDecoder/decode msg)))
;;    (route/not-found "not found")))


;; (defn init-http-server [{:keys [server-port bots]}]
;;   (let [stop-server (httpkit/run-server (site (app-routes bots))
;;                                         {:port server-port})]
;;     (Subscriptions/create
;;      (action*
;;       (fn -http-server-disposable []
;;         (@stop-server :timeout 100))))))
