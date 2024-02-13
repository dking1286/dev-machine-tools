(ns dev.dking.dev-machine-tools.ssh
  (:require [clojure.string :as string]))

(def block-separator-keywords #{"host" "match"})

(defn- has-host?
  [ssh-config-text host]
  (some #(string/starts-with? % (str "Host " host))
        (->> ssh-config-text
             (string/split-lines)
             (map string/trim))))

(defn- create-host!
  [ssh-config-file host identity-file user]
  (spit ssh-config-file
        (str "\n"
             "Host " host "\n"
             "  HostName 00.000.000.00\n"
             "  IdentityFile " identity-file "\n"
             "  User " user "\n")
        :append true))

(defn- parse-line
  [line]
  (let [[raw-key value] (-> line
                            string/trim
                            (string/split #"\s"))
        key (string/lower-case raw-key)]
    {:line line
     :key key
     :value value}))

(defn- config-keyword-is-one-of?
  [{:keys [key]} keywords]
  (contains? (set keywords) key))

(defn replace-hostname
  [ssh-config-text host new-hostname]
  (let [lines (string/split-lines ssh-config-text)]
    (loop [remaining lines
           result []
           in-target-host false]
      (if (empty? remaining)
        (string/join "\n" result)
        (let [{:keys [line key value]} (parse-line (first remaining))]
          (cond
            ;; We hit the target host, flip `in-target-host` to true and
            ;; continue.
            (and (= key "host") (= value host))
            (recur (rest remaining) (conj result line) true)

            ;; We hit the beginning of a "Host" or "Match" block that *isn't*
            ;; the target host. Flip `in-target-host` to false and continue.
            (block-separator-keywords key)
            (recur (rest remaining) (conj result line) false)

            ;; Found the HostName keyword under the target host, modify the
            ;; line with the new hostname and continue.
            (and in-target-host (= key "hostname"))
            (recur (rest remaining)
                   (conj result
                         (string/replace line #"(?i)(hostname) \S+" (str "$1 " new-hostname)))
                   true)

            ;; Continue with the current value of `in-target-host`.
            :else
            (recur (rest remaining) (conj result line) in-target-host)))))))
