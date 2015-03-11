(ns clipbot.plugins.jenkins
  (:require
   [clojure.string :as str]
   [rx.lang.clojure.core :as rx]

   [clj-http.client :as http]
   [clj-jenkins.job :as jenkins]

   [clipbot.plugin :as plugin]
   [clipbot.types :refer :all]))

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

(defn match [s rx]
  (->> s (re-seq rx) first second))

(def get-task
  #(match % #"@jenkins\s+(.*)\s*"))

(defn- category-type [category type]
  (fn -filter-msg [msg]
    (and (= (:category msg)) category
         (= (:type msg) type))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; jenkins parsers

(def display-help-event
  (chat-message
    (str "Available commands for @jenkins\n"
         (str/join "\n"
              (for [{:keys [name description]} jenkins-tasks]
                (str name " - " description))))))

;; this function can later be implemented with a zetta parser
(defn parse-chat-message [{:keys [payload] :as ev}]
  (let [[task-name & args] (str/split (->> payload (re-seq jenkins-regex) first second)
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


(defn chat-message-parser [{:keys [send-raw-event] :as ev}]
  (let [ev1 (parse-chat-message ev)]
    (send-raw-event ev1)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; jenkins tasks

(defn status-jenkins-job [{:keys [send-chat-message job-name] :as ev}]
  (send-chat-message (str "check last build of " job-name)))

(defn package-jenkins-job [{:keys [send-chat-message job-name] :as ev}]
  (send-chat-message (str "package jenkins job " job-name)))

(defn list-jenkins-jobs [{:keys [send-chat-message]}]
  (send-chat-message "list all jobs"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; jenkins-bot implementation


(defn init-jenkins-bot [subscribe observable]
  (let [chat-message-events  (rx/filter (category-type :chat :receive-message) observable)
        package-job-events   (rx/filter (category-type :jenkins :package) observable)
        status-job-events    (rx/filter (category-type :jenkins :status) observable)
        list-jobs-events     (rx/filter (category-type :jenkins :list) observable)]

    ;; print everything you receive
    (subscribe "echo-message-events"
               chat-message-events
               (fn echo-message [{:keys [send-chat-message payload]}]
                 (send-chat-message (str "echo: " payload))))

    ;; (subscribe "chat-message-parser" chat-message-events chat-message-parser)
    ;; (subscribe "status-jenkins-job"  status-job-events   status-jenkins-job)
    ;; (subscribe "package-jenkins-job" package-job-events  package-jenkins-job)

    ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; plugin registering

(plugin/register-plugin
  {:id "jenkins"
   :regex jenkins-regex
   :init init-jenkins-bot})
