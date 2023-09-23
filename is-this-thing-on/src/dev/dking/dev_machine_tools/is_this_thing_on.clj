(ns dev.dking.dev-machine-tools.is-this-thing-on
  (:import [com.google.api.gax.rpc NotFoundException PermissionDeniedException]
           [com.google.cloud.compute.v1 InstancesClient ListInstancesRequest]
           [com.google.cloud.secretmanager.v1 SecretManagerServiceClient GetSecretRequest SecretVersionName]
           [com.sendgrid SendGrid Request Method]
           [com.sendgrid.helpers.mail Mail]
           [com.sendgrid.helpers.mail.objects Email Content]
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

(defn- get-secret
  [project name alias]
  (with-open [client (SecretManagerServiceClient/create)]
    (try
      (let [secret-version-name (SecretVersionName/of project name alias)
            secret-version (.accessSecretVersion client secret-version-name)
            value  (-> secret-version .getPayload .getData .toStringUtf8)]
        {:result value})
      (catch NotFoundException e
        {:error {;; This exception is raised when the secret *name* exists and you have access to it,
                 ;; but the version is not found.
                 :type :secret-version-not-found
                 :exception e}})
      (catch PermissionDeniedException e
        {:error {;; If the secret name is not found, you get the same error as if
                 ;; you don't have access to the secret.
                 :type :permission-denied-or-secret-name-not-found
                 :exception e}}))))

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

  ;; Getting a secret
  (def secrets-client (SecretManagerServiceClient/create))
  (def secrets-req (-> (GetSecretRequest/newBuilder)
                       (.setName "projects/atelier-royal/secrets/foo")
                       (.build)))
  (def secret (.getSecret secrets-client secrets-req))
  (reflect secret)
  (.getName secret)
  (.getValue secret)
  (->> (reflect secret)
       :members
       (filter #(string/starts-with? (str (:name %)) "get"))
       (sort-by :name))
  (.getVersionAliasesMap secret)

  ;; Need to figure out how to tell it to access the secret version with alias "live"
  (def svn (SecretVersionName/of "atelier-royal" "foo" "live"))
  (def sv (.accessSecretVersion secrets-client svn))
  (def payload (.getPayload sv))
  (reflect payload)
  (.getData payload)
  (-> sv .getPayload .getData .toStringUtf8)
  (reflect sv)
  (reflect svn)

  (get-secret "atelier-royal" "foo" "live")
  (get-secret "atelier-royal" "foo" "blahblah")
  (get-secret "atelier-royal" "blah" "live")
  (get-secret "atelier-royal" "sendgrid-email-send-key" "latest")

  (def mail (Mail. (Email. "daniel.oliver.king@gmail.com")
                   "Testing"
                   (Email. "daniel.oliver.kind@gmail.com")
                   (Content. "text/plain" "Testing 123")))
  (def sg-api-key (:result (get-secret "atelier-royal" "sendgrid-email-send-key" "latest")))
  (def sg-client (SendGrid. sg-api-key))

  (def req (doto (Request.)
             (.setMethod Method/POST)
             (.setEndpoint "mail/send")
             (.setBody (.build mail))))
  (def res (.api sg-client req))
  (reflect res)
  (.getStatusCode res)
  (.getBody res))

