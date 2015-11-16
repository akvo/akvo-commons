(defproject org.akvo/commons "0.4.0-SNAPSHOT"
  :description "Akvo utility library"
  :url "http://akvo.org"
  :license {:name "GNU Affero General Public License v3.0"
            :url "https://www.gnu.org/licenses/agpl-3.0.html"}
  :signing {:gpg-key "devops@akvo.org"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [me.raynes/fs "1.4.6"]
                 [com.taoensso/timbre "3.3.1"]
                 [com.google.appengine/appengine-tools-sdk "1.9.9"]
                 [com.google.appengine/appengine-remote-api "1.9.9"]
                 [com.google.appengine/appengine-api-1.0-sdk "1.9.9"]
                 [ring/ring-core "1.3.1"]
                 [com.nimbusds/nimbus-jose-jwt "3.10"]
                 [com.stuartsierra/component "0.3.0"]])
