(ns dev.dking.dev-machine-tools.local-admin
  (:require [clojure.data.json :as json])
  (:import [com.google.cloud.compute.v1 InstancesClient ListInstancesRequest StartInstanceRequest]
           [com.google.protobuf.util JsonFormat]))

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
  [client]
  (let [list-instances-request (-> (ListInstancesRequest/newBuilder)
                                   (.setProject "atelier-royal")
                                   (.setZone "us-central1-a")
                                   (.setFilter (format "name=%s" "dev-machine"))
                                   (.build))
        list-instances-response (.list client list-instances-request)
        instance (first (.iterateAll list-instances-response))]
    (proto->edn instance)))

(defn- start-instance!
  [client]
  (let [request (-> (StartInstanceRequest/newBuilder)
                    (.setProject "atelier-royal")
                    (.setZone "us-central1-a")
                    (.setInstance "dev-machine")
                    (.build))
        result-future (.startAsync client request)
        result (.get result-future)]
    (proto->edn result)))

(defn up
  [& _]
  (let [instances-client (InstancesClient/create)
        instance (get-instance instances-client)]
    (if-not instance
      (println "No instance found.")
      (if (= (:status instance) "RUNNING")
        (println "Instance is already running, no need to start it.")
        (do
          (println "Starting instance...")
          (start-instance! instances-client))))))

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

  (up))

