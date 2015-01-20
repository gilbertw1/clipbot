(ns clipbot.bot
  (:require [rx.lang.clojure.core :as rx])
  (:import [rx.subscriptions Subscriptions CompositeSubscription]))

(defrecord Bot [id conn-conf conn plugins subject subscription handler])

(defn- get-handlers [plugin]
  (if-let [handlers (:handlers plugin)]
    handlers
    [plugin]))


;; In here, I would prefer to have a function in the plugin that receives
;; a filtered observable (by the regex), and returns a list of
;; disposables, this disposables are managed by the Bot record
(defn- create-handler [plugins]
  (let [handlers (->> plugins (map get-handlers) flatten)]
    (fn [responder {:keys [user msg]}]
      (doseq [{:keys [regex function]} handlers]
        (if (re-seq regex msg)
          ;; this makes the responder asynchronous in nature
          ;; this is quite interesting indeed
          (future
            (function responder user msg)))))))

(defn- create-subscription [subject plugins]
  (let [bot-subscription (CompositeSubscription.)]
    (doseq [{:keys [regex init]} plugins
            :let [observable subject ;; (rx/filter #(re-seq regex %) subject)
                  ]]
      (when init
        (let [subscription (init observable)]
          (.add bot-subscription subscription))))
    bot-subscription))

;; 
(defn create [{:keys [id connection plugins]} subject available-plugins]
  (let [plugin-list (doall (map #(get available-plugins %) plugins))]
    (Bot. id
          connection
          nil
          plugin-list
          subject
          (create-subscription subject plugin-list)
          (create-handler plugin-list))))
