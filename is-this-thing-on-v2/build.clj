(ns build
  (:require [clojure.tools.build.api :as b]))

(def deps-file "deps.edn")
(def src-dir "src")
(def java-src-dir "java_src")
(def target-dir "target")
(def class-dir (format "%s/classes" target-dir))
(def deployment-dir (format "%s/deployment" target-dir))
(def jar-file (format "%s/is-this-thing-on.jar" deployment-dir))

(def basis (b/create-basis {:project deps-file}))

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

(defn uberjar
  [& _]
  (clean)
  (compile-java)
  (compile-clojure)
  (pack-uberjar)
  (println "Done!"))

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

  (uberjar))
