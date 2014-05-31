(ns clipbot.bot)

(defrecord Bot [id conn-conf conn plugins handler])

(defn- create-handler [plugins]
  (fn [responder {:keys [user msg]}]
    (doseq [{:keys [regex function]} plugins]
      (if (re-seq regex msg)
        (future
          (function responder user msg))))))

(defn create [{:keys [id connection plugins]} available-plugins]
  (let [plugin-list (doall (map #(get available-plugins %) plugins))]
    (Bot. id connection nil plugin-list (create-handler plugin-list))))