(ns dev.dking.dev-machine-tools.gce
  (:require [babashka.process :refer [sh]]
            [cheshire.core :as json]
            [dev.dking.dev-machine-tools.interface :refer [running? start stop]]))

(def status {:terminated "TERMINATED"
             :running "RUNNING"})

(defn- gcloud
  [config & args]
  (let [{{project-id :project-id} :platform} config
        gcloud-args (conj args
                          (str "--project=" project-id)
                          "--format=json")
        {:keys [exit out err]} (apply sh "gcloud" gcloud-args)]
    (if (zero? exit)
      (json/parse-string-strict out)
      (throw (ex-info err {:type :gcloud-error})))))

(defmethod running? :gce
  [config]
  (let [current-status (-> (gcloud config "compute" "instances" "describe" (config :dev-machine-name))
                           (get "status"))]
    (= current-status (status :running))))

(defmethod start :gce
  [config]
  (gcloud config "compute" "instances" "start" (config :dev-machine-name)))

(defmethod stop :gce
  [config]
  (gcloud config "compute" "instances" "stop" (config :dev-machine-name)))