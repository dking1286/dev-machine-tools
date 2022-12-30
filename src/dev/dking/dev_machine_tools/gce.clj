(ns dev.dking.dev-machine-tools.gce
  (:require [babashka.process :refer [sh]]
            [cheshire.core :as json]
            [clojure.string :as string]
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

(defn- describe
  [config]
  (gcloud config "compute" "instances" "describe" (config :name)))

(defn- exists?
  [config]
  (try
    (describe config)
    true
    (catch clojure.lang.ExceptionInfo e
      (if (string/includes? (ex-message e) "Could not fetch resource")
        false
        (throw e)))))

(defmethod running? :gce
  [config]
  (let [current-status (-> (describe config)
                           (get "status"))]
    (= current-status (status :running))))

(defmethod start :gce
  [config]
  (gcloud config "compute" "instances" "start" (config :name)))

(defmethod stop :gce
  [config]
  (gcloud config "compute" "instances" "stop" (config :name)))

(comment
  (require '[dev.dking.dev-machine-tools.config :refer [get-config get-config-impl]])

  (def config (get-config-impl))

  config

  (gcloud config "compute" "instances" "describe" "i-dont-exist")
  (sh "gcloud" "compute" "instances" "describe" "i-dont-exist" "--project=atelier-royal" "--format=json")

  (describe config)

  (exists? config)
  (exists? (assoc config :name "i-dont-exist")))