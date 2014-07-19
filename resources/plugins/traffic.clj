(ns clipbot.plugins.traffic
  (:require [clipbot.plugin :as plugin]))

(defn match [s rx]
  (->> s (re-seq rx) first second))

(def main-site "http://www.511sc.org/")
(def image-url "http://sc.snapshots.iteriscdn.com/D6CAM")
(def image-extension ".jpg")
(defn get-cam [m] (match m #"\$traffic cam\s+(\d\d)"))

(plugin/register-plugin
  {:id "traffic"
   :handlers [{:regex #"\$traffic site"
               :function (fn [responder user msg]
                  (responder main-site))}
              {:regex #"\$traffic cam\s+(\d\d)"
               :function (fn [responder user msg]
                  (responder (str image-url (get-cam msg) image-extension)))}]})