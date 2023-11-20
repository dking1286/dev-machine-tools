(ns dev.dking.dev-machine-tools.is-this-thing-on
  (:import [com.google.api.client.googleapis.javanet GoogleNetHttpTransport]
           [com.google.api.client.json.gson GsonFactory]
           [com.google.api.services.gmail Gmail Gmail$Builder]
           [com.google.api.services.gmail.model Message]
           [com.google.cloud.compute.v1 InstancesClient ListInstancesRequest]
           [io.cloudevents CloudEvent]
           [java.io ByteArrayOutputStream]
           [java.util Properties]
           [javax.mail MessagingException Session]
           [javax.mail.internet InternetAddress MimeMessage]
           [org.apache.commons.codec.binary Base64]))

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
  (let [{:keys [result]} (running? "atelier-royal" "us-central1-a" "dev-machine")]
    (if result
      (println "The machine is running.")
      (println "The machine is NOT running."))))

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

  ;; Gmail api
  (def transport (GoogleNetHttpTransport/newTrustedTransport))
  (def json-factory (GsonFactory.))
  ;; (def gmail (Gmail. transport json-factory nil)) ;; Need to set an "application name" here
  (def gmail (-> (Gmail$Builder. transport json-factory nil)
                 (.setApplicationName "dev-machine")
                 (.build)))
  (reflect gmail)
  (def users (.users gmail))
  (reflect users)
  (def messages (.messages users))
  (reflect messages)

  (def mime-message (doto (MimeMessage. (Session/getDefaultInstance (Properties.) nil))
                      (.setFrom (InternetAddress. "daniel.oliver.king+atelier-royal@gmail.com"))
                      (.addRecipient javax.mail.Message$RecipientType/TO (InternetAddress. "daniel.oliver.king@gmail.com"))
                      (.setSubject "This is a test")
                      (.setText "Hello world")))
  (def buffer (ByteArrayOutputStream.))
  (.writeTo mime-message buffer)
  (def raw (Base64/encodeBase64URLSafeString (.toByteArray buffer)))

  (def message (.setRaw (Message.) raw))
  message
  ;; Next step: Actually send the message
  (def send-request (.send messages "me" message))
  (.execute send-request)
  ;; Not authorized. I'm going to abandon this approach and try another:
  ;; 1. Create a pubsub topic "is-this-thing-on-alerts"
  ;; 2. When the machine is on, put a message on the pubsub topic
  ;; 3. Create an alerting policy for if the number of unacked messages is > 0.
  )
