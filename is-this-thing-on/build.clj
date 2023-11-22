(ns build
  (:require [babashka.process :refer [shell]]
            [clojure.edn :as edn]
            [clojure.tools.build.api :as b]))

(def deps-file "deps.edn")
(def src-dir "src")
(def java-src-dir "java_src")
(def target-dir "target")
(def class-dir (format "%s/classes" target-dir))
(def deployment-dir (format "%s/deployment" target-dir))
(def jar-file (format "%s/is-this-thing-on.jar" deployment-dir))

(def basis (b/create-basis {:project deps-file}))

;; Base tasks ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn clean
  [& _]
  (println (format "Cleaning directory %s...", target-dir))
  (b/delete {:path target-dir}))

(defn compile-java
  [& _]
  (println (format "Compiling Java from %s into %s..." java-src-dir class-dir))
  (b/javac {:src-dirs ["java_src"]
            :class-dir class-dir
            :basis basis}))

(defn compile-clojure
  [& _]
  (println (format "Compiling Clojure from %s into %s..." src-dir class-dir))
  (b/compile-clj {:basis basis
                  :src-dirs [src-dir]
                  :class-dir class-dir}))

(defn pack-uberjar
  [& _]
  (println (format "Packaging code into %s..." jar-file))
  (b/uber {:basis basis
           :class-dir class-dir
           :uber-file jar-file}))

(defn deploy-uberjar
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

;; Composite tasks ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn uberjar
  [& _]
  (clean)
  (compile-java)
  (compile-clojure)
  (pack-uberjar))

(defn deploy
  [& _]
  (clean)
  (compile-java)
  (compile-clojure)
  (pack-uberjar)
  (deploy-uberjar))

(comment
  (clean)

  (b/compile-clj {:basis basis
                  :src-dirs ["src"]
                  :class-dir class-dir})

  (b/jar {:class-dir class-dir
          :jar-file jar-file})

  (b/javac {:src-dirs ["java_src"]
            :class-dir class-dir
            :basis basis})

  (uberjar)
  (deploy))
