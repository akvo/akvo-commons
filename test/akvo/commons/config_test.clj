;  Copyright (C) 2026 Stichting Akvo (Akvo Foundation)
;
;  This file is part of Akvo FLOW.
;
;  Akvo FLOW is free software: you can redistribute it and modify it under the terms of
;  the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
;  either version 3 of the License or any later version.
;
;  Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
;  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
;  See the GNU Affero General Public License included below for more details.
;
;  The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.

(ns akvo.commons.config-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [akvo.commons.config :as config]))

;; Every instance is identified by its app id: it keys the config map, and the
;; instance domain and p12 filename are derived from it. First-generation App Engine
;; descriptors declare it as <application>; second-generation ones are forbidden to,
;; so it has to come from somewhere else.

(deftest app-id-from-a-first-generation-descriptor
  (let [cfg (config/get-config
              (io/file "test/resources/instances/akvoflow-gen1/appengine-web.xml"))]
    (is (= "akvoflow-gen1" (:app-id cfg)))
    (is (= "akvoflow-gen1.appspot.com" (:domain cfg)))))

(deftest app-id-from-a-second-generation-descriptor
  (testing "no <application> element, so the directory name identifies the instance"
    (let [cfg (config/get-config
                (io/file "test/resources/instances/akvoflow-gen2/appengine-web.xml"))]
      (is (= "akvoflow-gen2" (:app-id cfg)))
      (is (= "akvoflow-gen2.appspot.com" (:domain cfg)))
      (is (= "gen2.akvoflow.org" (:alias cfg))))))

;; When two descriptors claim the same id, group-by collapses them and the counts
;; disagree. That is a config error worth reporting, and reporting it must not itself
;; throw -- which it did, for as long as this branch has existed.

(deftest inconsistent-config-is-reported-rather-than-thrown
  (config/set-config! "test/resources/duplicate-instances")
  (let [errors @config/errors-config]
    (is (seq errors) "an inconsistency should be recorded")
    (is (= {:processed-configs 1 :included-configs 2}
           (:config-data-inconsistency (first errors))))))
