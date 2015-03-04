(ns clipbot.chat.hipchat
  (:require
   [clojure.string :as str]
   [rx.lang.clojure.core :as rx]
   [disposables.core :refer [new-disposable* merge-disposable to-disposable]]
   [clipbot.types :refer :all])
  (:import
   [org.jivesoftware.smack ConnectionConfiguration XMPPConnection XMPPException PacketListener]
   [org.jivesoftware.smack.packet Message Presence Presence$Type]
   [org.jivesoftware.smackx.muc MultiUserChat]))

;; Transform an XMPP Message to a Clojure Map
(defn- message->map [#^Message m]
  (try
    {:category :chat
     :type :receive-message
     :payload (.getBody m)
     :user (-> m (.getFrom) (str/split #"/") second)}
    (catch Exception e
      (println "Received exception while parsing xmpp-message: (ignoring message)")
      (.printStackTrace e)
      {})))

;; Emits a message to the EventBus (subject) everytime a
;; chat message is received
(defn- xmpp-message-listener [subject room-id]
  (reify PacketListener
    (processPacket [_ packet]
      (let [message0 (message->map packet)
            message  (merge message0 {:room-id room-id
                                      :send-chat-message #(send-chat-message subject %)
                                      :send-raw-message  #(send-raw-message subject %)})]
        (try
          (send-raw-message subject message)
          (catch Exception e
            (.onError subject e)
            (.printStackTrace e)))))))

;; Setups the listener for HipChat XMPP Connection
(defn- setup-listen-messages [muc subject room-id]
  (let [listener (xmpp-message-listener subject room-id)]
    (.addMessageListener muc listener)
    (new-disposable* "HipChat receive chat listener"
                     #(.removeMessageListener muc listener))))

;; Setups message emiter for HipChat XMPP Connection
(defn- setup-send-messages [muc subject]
  (let [send-messages-observable (rx/filter (category-type? :chat :send-message)
                                            subject)
        subscription (rx/subscribe send-messages-observable
                                   #(.sendMessage muc (:payload %)))]
    (new-disposable* "HipChat send chat listener"
                     #(.unsubscribe subscription))))

;; Connects to particular HipChat room, and registers EventBus (subject)
;; to receive every message and emits it to intersted listeners
(defn- join-hipchat-room [conn room subject]
  (let [{room-id :id
         :keys [nick]} room
        muc (MultiUserChat. conn (str room-id "@conf.hipchat.com"))]

    (println "Joining room: " room-id " with nick: " nick)
    (.join muc nick)

    (merge-disposable (setup-listen-messages muc subject room-id)
                      (setup-send-messages muc subject))))

;; Setups the initial Connection to the HipChat XMPP Server
(defn- initialize-xmpp-connection [conn user pass]
  (.connect conn)
  (try
    (.login conn user pass "bot")
    (catch XMPPException e
      (throw (Exception. (str "Failed login with bot credentials for user: " user)))))
  (.sendPacket conn (Presence. Presence$Type/available)))

;; main: Starts a Chat with HipChat
(defn connect-hipchat [{:keys [user pass rooms]} subject]
  (let [conn (XMPPConnection. (ConnectionConfiguration. "chat.hipchat.com" 5222))]
    (initialize-xmpp-connection conn user pass)
    (merge-disposable
     (new-disposable* "HipChat Connection" #(.disconnect conn))
     (apply merge-disposable (mapv #(join-hipchat-room conn % subject) rooms)))))
