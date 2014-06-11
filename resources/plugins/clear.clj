(ns clipbot.plugins.clear
  (:require [clipbot.plugin :as plugin]))

(plugin/register-plugin
  {:id "clear"
   :regex #"\$clear"
   :function (fn [responder user msg]
               (dotimes [n 20] (responder "*")))})