(ns dev.dking.dev-machine-tools.local-admin
  (:require [clojure.data.json :as json])
  (:import [com.google.cloud.compute.v1 InstancesClient ListInstancesRequest StartInstanceRequest StopInstanceRequest]
           [com.google.protobuf.util JsonFormat]))

(def status {:running "RUNNING"
             :terminated "TERMINATED"})

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
  []
  (with-open [instances-client (InstancesClient/create)]
    (let [project "atelier-royal"
          zone "us-central1-a"
          instance-name "dev-machine"
          instance (get-instance instances-client
                                 {:project project
                                  :zone zone
                                  :instance-name instance-name})]
      (if-not instance
        (println "No instance found.")
        (if (= (instance :status) (status :running))
          (println "Instance is already running, no need to start it.")
          (do
            (println "Starting instance...")
            (start-instance! instances-client
                             {:project project
                              :zone zone
                              :instance-name instance-name})
            (println "Instance started!")))))))

(defn- down-impl
  []
  (with-open [instances-client (InstancesClient/create)]
    (let [project "atelier-royal"
          zone "us-central1-a"
          instance-name "dev-machine"
          instance (get-instance instances-client
                                 {:project project
                                  :zone zone
                                  :instance-name instance-name})]
      (if-not instance
        (println "No instance found.")
        (if (= (instance :status) (status :terminated))
          (println "Instance is already terminated, no need to stop it.")
          (do
            (println "Stopping instance...")
            (stop-instance! instances-client
                            {:project project
                             :zone zone
                             :instance-name instance-name})
            (println "Instance stopped!")))))))

(defn up
  [& _]
  (up-impl)
  (System/exit 0))

(defn down
  [& _]
  (down-impl)
  (System/exit 0))

(comment
  (require '[clojure.reflect :refer [reflect]])
  (import '[com.google.protobuf.util JsonFormat])
  (def client (InstancesClient/create))
  (def request (-> (ListInstancesRequest/newBuilder)
                   (.setProject "atelier-royal")
                   (.setZone "us-central1-a")
                   (.setFilter (format "name=%s" "dev-machine"))
                   (.build)))
  (def response (.list client request))
  (def instance (first (.iterateAll response)))
  (proto->edn instance)
  (reflect instance)
  (.getStatus instance)

  (def start-instance-request (-> (StartInstanceRequest/newBuilder)
                                  (.setProject "atelier-royal")
                                  (.setZone "us-central1-a")
                                  (.setInstance "dev-machine")
                                  (.build)))
  (def start-instance-future (.startAsync client start-instance-request))
  (def start-instance-result (.get start-instance-future))
  (proto->edn start-instance-result)
  (reflect start-instance-result)
  (reflect (.getErrorsList (.getError start-instance-result)))
  (-> start-instance-result
      (.getError)
      (.getErrorsList))
  (proto->edn nil)

  (up-impl)

  (get-instance client {:project "atelier-royal"
                        :zone "us-central1-a"
                        :instance-name "dev-machine"})

  (let [request (-> (StopInstanceRequest/newBuilder)
                    (.setProject "atelier-royal")
                    (.setZone "us-central1-a")
                    (.setInstance "dev-machine")
                    (.build))
        result-future (.stopAsync client request)
        result (.get result-future)]
    (proto->edn result)))
