(ns dev.dking.dev-machine-tools.config
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.spec.alpha :as s]))

(s/def ::non-empty-string (s/and string? not-empty))

(s/def ::project-id ::non-empty-string)

(s/def ::zone ::non-empty-string)
(s/def ::name ::non-empty-string)
(s/def ::instance (s/keys :req-un [::zone ::name]))

(s/def ::config (s/keys :req-un [::project-id ::instance]))

(defn- get-config-resource-or-throw
  []
  (if-let [config-resource (io/resource "config.edn")]
    config-resource
    (throw (ex-info (str "No configuration file found, please ensure that "
                         "your configuration is located at "
                         "resources/config.edn.")
                    {:reason :config-not-found}))))

(defn- parse-config-or-throw
  [config-str]
  (try
    (edn/read-string config-str)
    (catch Exception e
      (throw (ex-info (str "Could not parse resources/config.edn, "
                           "please ensure that this file contains "
                           "valid EDN data.")
                      {:reason :config-not-parseable}
                      e)))))

(defn- validate-config-or-throw
  [config]
  (when-not (s/valid? ::config config)
    (let [explanation (with-out-str (s/explain ::config config))
          explanation-data (s/explain-data ::config config)]
      (throw (ex-info (str "Configuration in resources/config.edn was "
                           "not valid. Explanation:\n\n"
                           explanation)
                      {:reason :config-not-valid
                       :explanation explanation-data}))))
  config)

(defn get-config
  []
  (-> (get-config-resource-or-throw)
      slurp
      parse-config-or-throw
      validate-config-or-throw))

(comment
  (def right {:project-id "foo"
              :instance {:zone "bar"
                         :name "baz"}})

  (def wrong {:project-id "blah"
              :instance {:zone ""}})

  (s/conform ::config right)
  (s/conform ::config wrong)
  (s/check-asserts?)

  (edn/read-string "{\"hello\": \"world\"}")
  (s/explain ::config right)
  (s/explain ::config wrong)
  (s/explain-data ::config wrong)

  (get-config)

  (validate-config-or-throw wrong))