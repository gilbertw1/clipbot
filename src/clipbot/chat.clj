(ns clipbot.chat
  (:require
   [clojure.string :as str]
   [disposables.core :refer [merge-disposable]]
   [rx.lang.clojure.core :as rx]
   [rx.lang.clojure.interop :refer [action*]]
   [disposables.core :refer [IDisposable]]
   [clipbot.bot :as bot]
   [clipbot.chat.hipchat :refer [connect-hipchat]])
  (:import
   [rx Subscription]
   [rx.subscriptions Subscriptions CompositeSubscription]))

;; Public

(defn connect [bot-conf subject]
  (let [{:keys [type conf]} (:connection bot-conf)]
    (println "Connecting Bot: " bot-conf)
    (condp = type
      "hipchat" (connect-hipchat conf subject)
      :else (throw (Exception. "Unknown chat type")))))

(defn init-chat [bot-configs plugins subject]
  (let [bot-disposables        (mapv #(bot/new-bot % subject plugins) bot-configs)
        ;; ^ Setups all different subscriptions to Rx streams
        connection-disposables (mapv #(connect % subject) bot-configs)
        ;; ^ Setups all connections to Chat resources (socket, etc.)
        ]
    (apply merge-disposable
           (concat connection-disposables bot-disposables))))
