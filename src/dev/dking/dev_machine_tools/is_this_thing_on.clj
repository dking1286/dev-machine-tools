(ns dev.dking.dev-machine-tools.is-this-thing-on
  (:require [dev.dking.dev-machine-tools.config :refer [get-config]])
  (:import [com.google.cloud.compute.v1 InstancesClient ListInstancesRequest]
           [com.google.cloud.pubsub.v1 Publisher]
           [com.google.protobuf ByteString]
           [com.google.pubsub.v1 PubsubMessage TopicName]
           [io.cloudevents CloudEvent]
           [java.util.concurrent TimeUnit]))

(defn- running?
  "Checks if the development machine is currently running."
  [project zone name]
  (with-open [client (InstancesClient/create)]
    (let [instance (-> client
                       (.list (-> (ListInstancesRequest/newBuilder)
                                  (.setProject project)
                                  (.setZone zone)
                                  (.setFilter (str "name=" name))
                                  (.build)))
                       (.iterateAll)
                       first)]
      (if (nil? instance)
        {:result false
         :error {:type :no-instance-found}}
        {:result (= (.getStatus instance) "RUNNING")}))))

(defn- publish-alert!
  "Publishes an alert that the dev machine is running to a pubsub topic."
  [project topic name]
  (let [publisher (-> (Publisher/newBuilder (TopicName/of project topic))
                      (.build))]
    (try
      (let [message-body (str name " is currently running.")
            message (-> (PubsubMessage/newBuilder)
                        (.setData (ByteString/copyFromUtf8 message-body))
                        (.build))
            result (.publish publisher message)
            published-message-id (.get result)]
        (println (str "Published message " published-message-id)))
      (finally
        (.shutdown publisher)
        (.awaitTermination publisher 1 TimeUnit/MINUTES)))))

(defn accept
  "Main entry point for the GCP Cloud Function."
  [^CloudEvent _]
  (let [{project :project-id
         {vm-zone :zone
          vm-name :name} :instance
         {alert-topic :alert-topic} :idle-monitoring} (get-config)
        {:keys [result]} (running? project vm-zone vm-name)]
    (if result
      (do
        (println "The machine is currently running, publishing alert.")
        (publish-alert! project alert-topic vm-name))
      (println "The machine is NOT running, not publishing an alert."))))
