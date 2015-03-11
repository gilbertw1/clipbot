(ns clipbot.plugins.clojure
  (:require [clipbot.plugin :as plugin]
            [clojail.core :as jail]
            [clojail.jvm :as jailvm]
            [clojail.testers :as tst]))

(def sbox (jail/sandbox tst/secure-tester :context (-> (jailvm/permissions (java.net.SocketPermission. "*:*" "connect,resolve")) jailvm/domain jailvm/context)))

(def clj-eval-rx #"^\s*,(.*)")

(defn try-evaluate [expr]
  (try
    (sbox (jail/safe-read expr))
    (catch Exception e (str "evaluation failed (" e ")"))))

(plugin/register-plugin
  {:id "clojure"
   :regex #"^,(.*)"
   :function (fn [responder user msg]
               (println "Evaluating: " (try-evaluate (subs msg 1)))
               (responder (str (try-evaluate (subs msg 1)))))})