(ns dev.dking.dev-machine-tools
  (:require [dev.dking.dev-machine-tools.config :refer [get-config]]
            [dev.dking.dev-machine-tools.gce]
            [dev.dking.dev-machine-tools.interface :refer [start stop running?]]))

(comment
  (running? (get-config))

  (start (get-config))

  (stop (get-config)))