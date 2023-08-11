(ns dev.dking.dev-machine-tools.is-this-thing-on
  (:import [com.google.cloud.compute.v1 InstancesClient ListInstancesRequest]
           [io.cloudevents CloudEvent]))

(defn- running?
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

(defn accept
  [^CloudEvent _]
  (println "hello world!")
  (let [{:keys [result]} (running? "atelier-royal" "us-central1-a" "dev-machine")]
    (if result
      (println "The machine is running.")
      (println "The machine is NOT running."))))

(comment
  ;; Extra requires for development
  (require '[clojure.reflect :refer [reflect]])

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
  (running? "atelier-royal" "us-central1-a" "dev-machine"))