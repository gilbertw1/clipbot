(ns clipbot.chat
  (:require
   [clojure.string :as str]
   [rx.lang.clojure.core :as rx]
   [rx.lang.clojure.interop :refer [action*]]
   [clipbot.bot :as bot])
  (:import
   [org.jivesoftware.smack ConnectionConfiguration XMPPConnection XMPPException PacketListener]
   [org.jivesoftware.smack.packet Message Presence Presence$Type]
   [org.jivesoftware.smackx.muc MultiUserChat]
   [rx Subscription]
   [rx.subscriptions Subscriptions CompositeSubscription]))

(defprotocol Chat-Connection
  (send-message [this channel msg])
  (join [this channel])
  (list-channels [this])
  (quit [this]))

(declare send-hipchat-message)
(declare join-hipchat-room)

(defrecord HipChat-Connection [conn channels username handler]
  Subscription
  (unsubscribe [_]
    (println "call unsubscribe on Hipchat-Connection")
    (.disconnect conn))
  (isUnsubscribed [_]
    (not (.isConnected conn)))

  Chat-Connection
  (send-message [_ channel-id msg]
    (send-hipchat-message conn (get channels channel-id) msg))
  (join [_ channel]
    (join-hipchat-room conn channel handler))
  (list-channels [_] channels)
  (quit [this]
    (.unsubscribe this)))

;; HipChat

(defn- message->map [#^Message m]
  (try
    {:msg (.getBody m)
     :user (-> m (.getFrom) (str/split #"/") second)}
    (catch Exception e (println e) {})))

(defn- with-message-map [room-id subject handler]
  (fn [muc packet]
    (let [message0 (message->map #^Message packet)
          message (merge message0 {:type :receive-message
                                   :category :chat
                                   :room-id room-id
                                   :send-message #(.onNext subject
                                                           {:type :send-message
                                                            :category :chat
                                                            :msg %})
                                   :emit-event #(.onNext subject %)})]
      (try
       (handler muc message)
       (.onNext subject message)
       (catch Exception e
         (.onError subject e)
         (println e))))))

(defn- wrap-handler [handler]
  (fn [muc message]
    (let [respond #(.sendMessage muc %)]
      (handler respond message))))

(defn- packet-listener [conn processor]
  (reify PacketListener
    (processPacket [_ packet]
      (processor conn packet))))

(defn- chat-send-msg? [msg]
  (and (= (:type msg) :send-message)
       (= (:category msg) :chat)))

(defn- join-hipchat-room [conn room subject handler]
  (let [{:keys [id nick]} room
        muc (MultiUserChat. conn (str id "@conf.hipchat.com"))]
    (println "Joining room: " id " with nick: " nick)
    (.join muc nick)
    (.addMessageListener muc (packet-listener muc (with-message-map id subject (wrap-handler handler))))
    (let [send-msg-disposable (rx/subscribe (rx/filter chat-send-msg? subject)
                                            #(.sendMessage muc (:msg %)))]
      (merge room {:conn muc
                   :sender-disposable send-msg-disposable}))))

(defn- initialize-xmpp-connection [conn user pass]
  (.connect conn)
  (try
    (.login conn user pass "bot")
    (catch XMPPException e
      (throw (Exception. (str "Failed login with bot credentials for user: " user)))))
  (.sendPacket conn (Presence. Presence$Type/available)))

(defn- send-hipchat-message [conn channel msg]
  (-> channel :conn (.sendMessage msg)))

(defn- connect-hipchat [{:keys [user pass rooms]} subject handler]
  (let [conn (XMPPConnection. (ConnectionConfiguration. "chat.hipchat.com" 5222))]
    (initialize-xmpp-connection conn user pass)
    (let [connected-channels (map #(join-hipchat-room conn % subject handler) rooms)]
      (HipChat-Connection. conn (group-by :id connected-channels) user handler))))

;; IRC

(defn- connect-irc [conf handler]
  (throw (Exception. "IRC chat not implemented.")))

;; Public

(defn connect [{:keys [type conf]} subject handler]
  (condp = type
    "hipchat" (connect-hipchat conf subject handler)
    "irc" (connect-irc conf handler)
    :else (throw (Exception. "Unknown chat type"))))

(defn connect-bot [bot]
  (println "Connecting Bot: " bot)
  (assoc bot :conn (connect (:conn-conf bot)
                            (:subject bot)
                            (:handler bot))))

(defn init-chat [bot-configs plugins subject]
  (let [chat-subscription (CompositeSubscription.)
        disconnected-bots (mapv #(bot/create % subject plugins) bot-configs)
        connected-bots    (mapv connect-bot disconnected-bots)]
    (doseq [bot connected-bots]
      (.add chat-subscription bot))
    chat-subscription))
