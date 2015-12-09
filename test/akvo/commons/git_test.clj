;  Copyright (C) 2013-2015 Stichting Akvo (Akvo Foundation)
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

(ns akvo.commons.git-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [me.raynes.fs :as fs]
            [akvo.commons.git :refer :all]))

(def ^:dynamic *repo-dir*)

(use-fixtures :each
  (fn [f]
    (let [dir (fs/temp-dir "akvo-commons")]
      (binding [*repo-dir* (str dir)]
        (f)
        (fs/delete-dir dir)))))

(def clone-url "https://github.com/akvo/akvo-commons")

(deftest git-tests
  (testing "clone/pull repo"
    (is (nil? (clone *repo-dir* clone-url)))
    (is (nil? (pull *repo-dir* clone-url))))
  (testing "clone or pull repo"
    (is (nil? (clone-or-pull *repo-dir* clone-url)))))
