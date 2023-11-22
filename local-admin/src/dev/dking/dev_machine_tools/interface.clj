(ns dev.dking.dev-machine-tools.interface)

(defmulti running?
  (fn [config] (get-in config [:platform :type])))

(defmulti start
  (fn [config] (get-in config [:platform :type])))

(defmulti stop
  (fn [config] (get-in config [:platform :type])))
