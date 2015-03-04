(ns clipbot.bot-test
  (:require [clipbot.bot :refer :all]
            [clipbot.types :refer [chat-message]]
            [clojure.test :refer :all]
            [disposables.core :refer [to-disposable dispose]])
  (:import
   [rx.subjects PublishSubject]))

(defn test-plugin-setup [{:keys [plugin-regex event-bus register-plugin?]}]
  (let [st-atom (atom [])
        plugin-id "test-plugin"

        send-msg #(.onNext event-bus (chat-message %))

        ;; this creates a subscription side-effect
        bot
        (new-bot {:id "test"
                  :plugins (if register-plugin? [plugin-id] [])}
                 event-bus
                 ;; plugin map
                 {plugin-id {:id plugin-id
                             :regex plugin-regex
                             :init
                             (fn test-plugin-init [subscribe observable]
                               (subscribe observable
                                          #(swap! st-atom conj %)
                                          #(.printStackTrace %)))
                             }})]
    [st-atom send-msg bot]))

(deftest new-bot-test
  (let [event-bus (PublishSubject/create)
        plugin-regex #"^test-plugin"]

    (testing "doesn't subscribe to available plugins if not indicated by bot config"
      (let [[st-atom send-msg bot]
            (test-plugin-setup {:event-bus event-bus
                                :plugin-regex plugin-regex})]
        (are [msg received-count]
          (do
            (send-msg msg)
            (is (= (count @st-atom) received-count)))
          "test-plugin you should see this" 0
          "test-plugin you should see this" 0
          "you should not see me" 0)))

    (testing "subscribes to available plugins if indicated by bot config"
      (let [[st-atom send-msg bot]
            (test-plugin-setup {:event-bus event-bus
                                :plugin-regex plugin-regex
                                :register-plugin? true})]
        (are [msg received-count]
          (do
            (send-msg msg)
            (is (= (count @st-atom) received-count)))
           "test-plugin you should see this" 1
           "test-plugin you should see that" 2
           ;; ignores this message
           "you should not see me" 2
           "test-plugin last one" 3)))

    (testing "ignores all subscribed messages once it is disposed"
      (let [[st-atom send-msg bot]
            (test-plugin-setup {:event-bus event-bus
                                :plugin-regex plugin-regex
                                :register-plugin? true})]

        ;; before any message
        (is (= (count @st-atom) 0))

        ;; message that we care about
        (send-msg "test-plugin you should see this")
        (is (= (count @st-atom) 1))

        ;; we won't care about messages like the previous one any more
        (dispose (to-disposable bot))

        ;; message won't be listened (count doesn't change)
        (send-msg "test-plugin you should see this")
        (is (= (count @st-atom) 1))))

    ))
