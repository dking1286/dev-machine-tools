(ns dev.dking.dev-machine-tools.local-admin
  (:require [babashka.process :refer [shell]]
            [clojure.data.json :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.tools.build.api :as b]
            [dev.dking.dev-machine-tools.config :refer [get-config]])
  (:import [com.google.cloud.compute.v1
            InstancesClient
            ListInstancesRequest
            StartInstanceRequest
            StopInstanceRequest]
           [com.google.protobuf.util JsonFormat]))

(def deps-file "deps.edn")
(def src-dir "src")
(def java-src-dir "java_src")
(def target-dir "target")
(def class-dir (format "%s/classes" target-dir))
(def deployment-dir (format "%s/deployment" target-dir))
(def idle-monitoring-jar-file (format "%s/is-this-thing-on.jar" deployment-dir))

(def basis (b/create-basis {:project deps-file}))

(def status {:running "RUNNING"
             :terminated "TERMINATED"})

;; Instance management helper functions ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- unsafe-proto->edn
  [proto]
  (let [printer (JsonFormat/printer)
        proto-json (.print printer proto)]
    (json/read-str proto-json :key-fn keyword)))

(defn- proto->edn
  [proto]
  (if (nil? proto)
    nil
    (unsafe-proto->edn proto)))

(defn- get-instance
  [client {:keys [project zone instance-name]}]
  (let [list-instances-request (-> (ListInstancesRequest/newBuilder)
                                   (.setProject project)
                                   (.setZone zone)
                                   (.setFilter (format "name=%s" instance-name))
                                   (.build))
        list-instances-response (.list client list-instances-request)
        instance (first (.iterateAll list-instances-response))]
    (proto->edn instance)))

(defn- start-instance!
  [client {:keys [project zone instance-name]}]
  (let [request (-> (StartInstanceRequest/newBuilder)
                    (.setProject project)
                    (.setZone zone)
                    (.setInstance instance-name)
                    (.build))
        result-future (.startAsync client request)
        result (.get result-future)]
    (proto->edn result)))

(defn- stop-instance!
  [client {:keys [project zone instance-name]}]
  (let [request (-> (StopInstanceRequest/newBuilder)
                    (.setProject project)
                    (.setZone zone)
                    (.setInstance instance-name)
                    (.build))
        result-future (.stopAsync client request)
        result (.get result-future)]
    (proto->edn result)))

(defn- up-impl
  [{project :project-id {zone :zone instance-name :name} :instance}]
  (with-open [instances-client (InstancesClient/create)]
    (let [instance (get-instance instances-client
                                 {:project project
                                  :zone zone
                                  :instance-name instance-name})]
      (if-not instance
        (println (format (str "No instance named %s found in project "
                              "%s, zone %s.")
                         instance-name
                         project
                         zone))
        (if (= (instance :status) (status :running))
          (println "Instance is already running, no need to start it.")
          (do
            (println (format (str "Starting instance %s in project %s, "
                                  "zone %s.")
                             instance-name
                             project
                             zone))
            (start-instance! instances-client
                             {:project project
                              :zone zone
                              :instance-name instance-name})
            (println "Instance started!")))))))

(defn- down-impl
  [{project :project-id {zone :zone instance-name :name} :instance}]
  (with-open [instances-client (InstancesClient/create)]
    (let [instance (get-instance instances-client
                                 {:project project
                                  :zone zone
                                  :instance-name instance-name})]
      (if-not instance
        (println (format (str "No instance named %s found in project "
                              "%s, zone %s.")
                         instance-name
                         project
                         zone))
        (if (= (instance :status) (status :terminated))
          (println "Instance is already terminated, no need to stop it.")
          (do
            (println (format (str "Stopping instance %s in project %s, "
                                  "zone %s.")
                             instance-name
                             project
                             zone))
            (stop-instance! instances-client
                            {:project project
                             :zone zone
                             :instance-name instance-name})
            (println "Instance stopped!")))))))

;; Idle monitoring deployment helper functions ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- clean
  [& _]
  (println (format "Cleaning directory %s...", target-dir))
  (b/delete {:path target-dir}))

(defn- compile-java
  [& _]
  (println (format "Compiling Java from %s into %s..." java-src-dir class-dir))
  (b/javac {:src-dirs ["java_src"]
            :class-dir class-dir
            :basis basis}))

(defn- compile-clojure
  [& _]
  (println (format "Compiling Clojure from %s into %s..." src-dir class-dir))
  (b/compile-clj {:basis basis
                  :src-dirs [src-dir]
                  :class-dir class-dir
                  :ns-compile '[dev.dking.dev-machine-tools.is-this-thing-on]}))

(defn- pack-uberjar
  [& _]
  (println (format "Packaging code into %s..." idle-monitoring-jar-file))
  (b/uber {:basis basis
           :class-dir class-dir
           :uber-file idle-monitoring-jar-file}))

(defn- deploy-uberjar
  [& _]
  (println "Deploying GCP Cloud Function...")
  (shell (format "gcloud functions deploy is_this_thing_on
                  --gen2
                  --region=\"us-central1\"
                  --runtime=\"java17\"
                  --memory=\"512MiB\"
                  --source=\"%s\"
                  --entry-point=\"dev.dking.dev_machine_tools.is_this_thing_on.CloudFunction\"
                  --trigger-topic=\"is-this-thing-on\""
                 deployment-dir)))

;; Public CLI commands ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn up
  [& _]
  (up-impl (get-config))
  (System/exit 0))

(defn down
  [& _]
  (down-impl (get-config))
  (System/exit 0))

(defn deploy-idle-monitoring
  [& _]
  (clean)
  (compile-java)
  (compile-clojure)
  (pack-uberjar)
  (deploy-uberjar))

(comment
  (slurp (io/resource "blah.edn"))
  (get-config)
  (def rdr (java.io.PushbackReader. (io/reader (io/resource "blah.edn"))))
  (def rdr (java.io.PushbackReader. (io/reader (io/resource "config.edn"))))
  (edn/read rdr))

