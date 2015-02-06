(ns clipbot.plugins.jenkins
  (:require
   [clojure.string :as str]
   [rx.lang.clojure.core :as rx]
   [clipbot.plugin :as plugin]
   [clj-http.client :as http]
   [clj-jenkins.job :as jenkins]
   )
  (:import [rx Observable]
           [rx.subscriptions CompositeSubscription]))

;; credentials come from env vars
;; - JENKINS_URL
;; - JENKINS_USERNAME
;; - JENKINS_API_TOKEN

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; constants

(def jenkins-regex #"^@jenkins\s+(.*?)\s*$")

(def HELP
  {:name "help"
   :args "args parser here"
   :description "Show this help"})

(def LIST
  {:name "list"
   :args "args parser here"
   :description "List all available jobs"})

(def STATUS
  {:name "status"
   :args ":args parser here"
   :description "Get status for given job"})

(def PACKAGE
  {:name "package"
   :args "args parser here"
   :description "Creates a package build for given job"})

(def jenkins-tasks [HELP LIST PACKAGE])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; utils

(defn to-observable [io-action]
  (fn -to-observable-wrapper [& args]
    (rx/observable*
     (fn -to-observable-observer-fn [^rx.Subscriber observer]
       (try
         (let [result (apply io-action args)]
           (rx/on-next observer result)
           (rx/on-completed observer))
         (catch Exception err
           (rx/on-error observer err)))))))

(def http-get-observable
  (to-observable http/get))

(defn mk-subscribe [^CompositeSubscription subscription]
  (fn ub-subsribe [& args]
    (.add subscription (apply rx/subscribe args))))

(defn init-bot [initializer]
  (let [subscription (CompositeSubscription.)
        subscribe (mk-subscribe subscription)]
    (fn -init-bot [observable]
      (initializer subscribe observable)
      subscription)))

(defn match [s rx]
  (->> s (re-seq rx) first second))

(def get-task
  #(match % #"@jenkins\s+(.*)\s*"))

;; global util fn
(defn send-message-event [msg]
  {:type :send-message
   :category :chat
   :msg msg})

(defn- category-type [category type]
  (fn -filter-msg [msg]
    (and (= (:category msg)) category
         (= (:type msg) type))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; jenkins parsers

(def display-help-event
  (send-message-event
    (str "Available commands for @jenkins\n"
         (str/join "\n"
              (for [{:keys [name description]} jenkins-tasks]
                (str name " - " description))))))

;; this function can later be implemented with a zetta parser
(defn parse-chat-message [{:keys [msg] :as ev}]
  (let [[task-name & args] (str/split (->> msg (re-seq jenkins-regex) first second)
                                   #"\s+") ]
    (merge ev
           (cond
             (= task-name (:name PACKAGE))
             {:category :jenkins
              :type :package
              :job-name (first args)}

             (= task-name (:name STATUS))
             {:category :jenkins
              :type :status
              :job-name (first args)}

             :else
             display-help-event))))


(defn chat-message-parser [{:keys [emit-event] :as ev}]
  (let [ev1 (parse-chat-message ev)]
    (emit-event ev1)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; jenkins tasks

(defn status-jenkins-job [{:keys [send-message job-name] :as ev}]
  (send-message (str "check last build of " job-name)))

(defn package-jenkins-job [{:keys [send-message job-name] :as ev}]
  (send-message (str "package jenkins job " job-name)))

(defn list-jenkins-jobs [{:keys [send-message]}]
  (send-message (str "list all jobs")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; jenkins-bot implementation


(defn init-jenkins-bot [subscribe observable]
  (let [chat-message-events  (rx/filter (category-type :chat :receive-message) observable)
        package-job-events   (rx/filter (category-type :jenkins :package) observable)
        status-job-events    (rx/filter (category-type :jenkins :status) observable)
        list-jobs-events     (rx/filter (category-type :jenkins :list) observable)]

    ;; print everything you receive
    (subscribe chat-message-events
               (fn echo-message [{:keys [send-message msg]}]
                 (send-message (str "echo: " msg))))

    (subscribe chat-message-events chat-message-parser)
    (subscribe status-job-events   status-jenkins-job)
    (subscribe package-job-events  package-jenkins-job)
    ;; (subscribe list-jobs-events    list-jenkins-jobs)
    ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; plugin registering

(plugin/register-plugin
  {:id "jenkins"
   :regex jenkins-regex
   :function #(do nil)
   :init (init-bot init-jenkins-bot)})
