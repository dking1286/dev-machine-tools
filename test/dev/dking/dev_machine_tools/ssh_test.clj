(ns dev.dking.dev-machine-tools.ssh-test
  (:require [clojure.test :refer [deftest is testing]]
            [dev.dking.dev-machine-tools.ssh :as ssh]
            [clojure.string :as string]))

(deftest replace-hostname-test
  (let [config (string/join "\n"
                            ["Host foo"
                             "  Hostname foobar"
                             "  SomeOtherFlag bazbang"
                             "Match bar"
                             "  Hostname blah"
                             "  SomeOtherFlag blah"
                             "Host yes"
                             "  HostName yesyes"
                             "  SomeOtherFlag yesyes"
                             "Match baz"
                             "  SomeOtherFlag baz"
                             "  Hostname baz"])]
    (testing "Does nothing when the hostname is not found"
      (let [expected config
            actual (ssh/replace-hostname config "doesnotexist" "modified")]
        (is (= expected actual))))
    (testing "Replaces the hostname under the specified host"
      (let [expected  (string/join "\n"
                                   ["Host foo"
                                    "  Hostname foobar"
                                    "  SomeOtherFlag bazbang"
                                    "Match bar"
                                    "  Hostname blah"
                                    "  SomeOtherFlag blah"
                                    "Host yes"
                                    "  HostName modified"
                                    "  SomeOtherFlag yesyes"
                                    "Match baz"
                                    "  SomeOtherFlag baz"
                                    "  Hostname baz"])
            actual (ssh/replace-hostname config "yes" "modified")]
        (is (= expected actual))))))