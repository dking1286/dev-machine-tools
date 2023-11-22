(ns dev.dking.dev-machine-tools.is-this-thing-on
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
  (let [project "atelier-royal"
        vm-zone "us-central1-a"
        vm-name "dev-machine"
        alert-topic "is-this-thing-on-alerts"
        {:keys [result]} (running? project vm-zone vm-name)]
    (if result
      (do
        (println "The machine is currently running, publishing alert.")
        (publish-alert! project alert-topic vm-name))
      (println "The machine is NOT running, not publishing an alert."))))

(comment
  ;; Extra requires for development
  (require '[clojure.reflect :refer [reflect]])
  (require '[clojure.string :as string])

  ;; Getting a client
  (def client (InstancesClient/create))
  (reflect client)

  ;; Creating a request
  (def req (-> (ListInstancesRequest/newBuilder)
               (.setProject "atelier-royal")
               (.setZone "us-central1-a")
               (.setFilter "name=dev-machine")
               (.build)))
  (reflect req)

  ;; Sending the request and inspecting the response
  (def resp (.list client req))
  (def instances (seq (.iterateAll resp)))
  (println instances)
  (reflect (first instances))
  (.getStatus (first instances))

  ;; Using the implementation function
  (running? "atelier-royal" "us-central1-a" "dev-machine")

  ;; Publishing to pubsub
  (def topic-name (TopicName/of "atelier-royal" "is-this-thing-on-alerts"))

  (def publisher (-> (Publisher/newBuilder topic-name)
                     (.build)))
  (def message (-> (PubsubMessage/newBuilder)
                   (.setData (ByteString/copyFromUtf8 "Hello world"))
                   (.build)))
  (def result (.publish publisher message))
  (.get result)

  (publish-alert! "atelier-royal" "is-this-thing-on-alerts" "dev-machine"))
