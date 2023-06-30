(ns dev.dking.dev-machine-tools.config)

(defn get-config-impl
  []
  {:platform {:type :gce
              :project-id "atelier-royal"}
   :dev-machine-name "dev-machine"})

(def get-config (memoize get-config-impl))