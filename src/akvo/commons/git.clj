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

(ns akvo.commons.git
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]))

(defn ensure-directory [dir]
  (.mkdirs (io/file dir)))

(defn repo-name [^String clone-url]
  (subs clone-url (inc (.lastIndexOf clone-url "/"))))

(defn pull [repo-dir ^String clone-url]
  (let [repo (repo-name clone-url)
        exit-code (:exit
                   (shell/with-sh-dir (str repo-dir "/" repo)
                     (shell/sh "git" "pull")))]
    (when-not (zero? exit-code)
      (throw (ex-info "Failed to pull repo"
                      {:repo repo
                       :exit-code exit-code})))))

(defn clone [repo-dir clone-url]
  (ensure-directory repo-dir)
  (let [exit-code (:exit
                   (shell/with-sh-dir repo-dir
                     (shell/sh "git" "clone" clone-url)))]
    (when-not (zero? exit-code)
      (throw (ex-info "Failed to clone repo"
                      {:repo-dir repo-dir
                       :clone-url clone-url
                       :exit-code exit-code})))))

(defn clone-or-pull [repo-dir clone-url]
  (let [repo (repo-name clone-url)]
    (if (.exists (io/file repo-dir (repo-name clone-url)))
      (pull repo-dir clone-url)
      (clone repo-dir clone-url))))
