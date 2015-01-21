(ns clipbot.chat
  (:require [clojure.string :as str])
  (:import
   [org.jivesoftware.smack ConnectionConfiguration XMPPConnection XMPPException PacketListener]
   [org.jivesoftware.smack.packet Message Presence Presence$Type]
   [org.jivesoftware.smackx.muc MultiUserChat]))

(defprotocol Chat-Connection
  (send-message [this channel msg])
  (join [this channel])
  (list-channels [this])
  (quit [this]))

(declare send-hipchat-message)
(declare join-hipchat-room)

(defrecord HipChat-Connection [conn channels username handler]
  Chat-Connection
    (send-message [this channel-id msg] (send-hipchat-message conn (get channels channel-id) msg))
    (join [this channel] (join-hipchat-room conn channel handler))
    (list-channels [this] channels)
    (quit [this] (println "Hipchat Quit Not Implemented!")))

;; HipChat

(defn- message->map [#^Message m]
  (try
    {:msg (.getBody m)
     :user (-> m (.getFrom) (str/split #"/") second)}
    (catch Exception e (println e) {})))

(defn- with-message-map [room-id subject handler]
  (fn [muc packet]
    (let [message0 (message->map #^Message packet)
          message (merge message0 {:room-id room-id
                                   :type :chat
                                   :respond #(.sendMessage muc %)
                                   :emit-event #(.onNext subject %)})]
      (try
       (handler muc message)
       (.onNext subject message)
       (catch Exception e (println e))))))

(defn- wrap-handler [handler]
  (fn [muc message]
    (let [respond #(.sendMessage muc %)]
      (handler respond message))))

(defn- packet-listener [conn processor]
  (reify PacketListener
    (processPacket [_ packet]
      (processor conn packet))))

(defn- join-hipchat-room [conn room subject handler]  
  (let [{:keys [id nick]} room        
        muc (MultiUserChat. conn (str id "@conf.hipchat.com"))]
    (println "Joining room: " id " with nick: " nick)
    (.join muc nick)
    (.addMessageListener muc (packet-listener muc (with-message-map id subject (wrap-handler handler))))
    (assoc room :conn muc)))

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
