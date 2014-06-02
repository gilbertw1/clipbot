(ns clipbot.bot)

(defrecord Bot [id conn-conf conn plugins handler])

(defn- get-handlers [plugin]
  (if-let [handlers (:handlers plugin)]
    handlers
    [plugin]))

(defn- create-handler [plugins]
  (let [handlers (->> plugins (map get-handlers) flatten)]
    (fn [responder {:keys [user msg]}]
      (doseq [{:keys [regex function]} handlers]
        (if (re-seq regex msg)
          (future
            (function responder user msg)))))))

(defn create [{:keys [id connection plugins]} available-plugins]
  (let [plugin-list (doall (map #(get available-plugins %) plugins))]
    (Bot. id connection nil plugin-list (create-handler plugin-list))))