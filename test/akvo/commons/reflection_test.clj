;  Copyright (C) 2015 Stichting Akvo (Akvo Foundation)
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

(ns akvo.commons.reflection-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.java.shell :as shell]))

(def lein-path (or (System/getenv "LEIN_PATH")
                   (str (System/getProperty "user.home") "/.local/bin/lein")))

(deftest warnings
  (testing "Reflection warning"
    (let [result (:err (shell/sh lein-path "check"))]
      (is (= false (.contains ^String result "Reflection warning, akvo/commons/"))))))
