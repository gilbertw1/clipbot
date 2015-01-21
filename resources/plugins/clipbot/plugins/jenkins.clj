(ns clipbot.plugins.jenkins
  (:require
   [clojure.string :as str]
   [rx.lang.clojure.core :as rx]
   [clipbot.plugin :as plugin]
   [jenko.core :as jenko])
  (:import [rx Observable]
           [rx.subscriptions CompositeSubscription]))

;; credentials come from env vars
;; - JENKINS_URL
;; - JENKINS_USER
;; - JENKINS_TOKEN

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; constants

(def HELP {:name "help"
           :description "Show this help"})

(def LIST-JOBS {:name "list"
                :description "List all available jobs"})

(def LEAD-SERVICE-PACKAGE
  { :name "package-leads-service"
    :description "Build the leads service project"})

(def jenkins-tasks [HELP LIST-JOBS LEAD-SERVICE-PACKAGE])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; utils

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

(defn display-help [responder]
  (responder "Available commands for @jenkins\n")
  (responder
   (str/join "\n"
             (for [{:keys [name description]} jenkins-tasks]
               (str name " - " description)))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; jenkins parsers

(defn chat-message-parser [{:keys [emit-event msg] :as ev}]
  (let [[action & args] (str/split (->> msg (re-seq #"^@jenkins\s+(.*?)\s*$") first second)
                                   #"\s+") ]
    (condp = action
      "package" (emit-event (merge ev {:type :package
                                       :job-name (first args)}))
      "list"    (emit-event (merge ev {:type :list-jobs})))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; jenkins-bot implementation


(defmulti jenkins-bot
  (fn jenkins-bot-dispatcher [_ _ msg]
    (let [dispatch-value (get-task msg)]
      dispatch-value))
  :default "unknown")

(defmethod jenkins-bot (:name LIST-JOBS) [responder _ _]
  (responder "Fetching list of jenkins jobs...")
  (responder (str/join "\n" (mapv :name (jenko/jobs)))))

(defmethod jenkins-bot (:name HELP) [responder _ _]
  (display-help responder))

(defmethod jenkins-bot (:name LEAD-SERVICE-PACKAGE) [responder _ _]
  (responder "packaging leads-service"))

(defmethod jenkins-bot "unknown" [responder _ msg]
  (responder (str  "ERROR: unknown task " (get-task msg) "\n"))
  (display-help responder))

(defn init-jenkins-bot [subscribe observable]
  (let [chat-messages (rx/filter #(= (:type %) :chat) observable)
        goodbye-observable (rx/filter #(re-seq #"goodbye" (:msg %)) observable)]

    ;; say goodbye to people
    (subscribe goodbye-observable
               (fn say-goodbye [{:keys [respond user]}]
                 (respond (str "Goodbye " (first (str/split user #" "))))))

    (subscribe chat-messages chat-message-parser)

    ;; print everything you receive
    (subscribe observable #(println %))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; plugin registering

(plugin/register-plugin
  {:id "jenkins"
   :regex #"@jenkins\s+(.*)\s*"
   :function #(do nil) ;; jenkins-bot
   :init (init-bot init-jenkins-bot)})
