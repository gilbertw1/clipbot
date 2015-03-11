(ns clipbot.types
  (:require [disposables.core :refer [new-disposable* IToDisposable]])
  (:import
   [disposables.core Disposable]
   [rx Subscription]
   [rx.subjects Subject]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Interop

(extend-type Subscription
  IToDisposable
  (to-disposable [self]
    (new-disposable* "rx.subscription" #(.unsubscribe self))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Bot

;; (defrecord Bot [id plugins ^Disposable disposable ^Subject subject]
;;   IToDisposable
;;   (to-disposable [_] disposable))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Chat record and protocols

;; (defrecord HipChat-Connection [^Disposable disposable channels username]
;;   (IToDisposable [_] disposable))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Util general functions

(defn category-type? [category type]
  (fn -category-type? [msg]
    (and (= (:category msg)) category
         (= (:type msg) type))))

(defn chat-message [payload]
  {:category :chat
   :type :send-message
   :payload payload})

;; Check if an outbound clipbot message has valid format
(defn valid-raw-message? [msg]
  (and
   (:category msg)
   (:type msg)))

;; Sends a chat message to HipChat
;; NOTE: payload is *always* transformed to string
(defn send-chat-message [subject payload]
  (.onNext subject
           (chat-message (str payload))))

(defn send-raw-message [subject msg]
  {:pre [(valid-raw-message? msg)]}
  (.onNext subject msg))
