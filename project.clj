(defproject org.akvo/commons "0.4.5"
  :description "Akvo utility library"
  :url "http://akvo.org"
  :license {:name "GNU Affero General Public License v3.0"
            :url "https://www.gnu.org/licenses/agpl-3.0.html"}
  :signing {:gpg-key "devops@akvo.org"}
  :deploy-repositories [["releases" :clojars]
                        ["snapshots" :clojars]]
  :dependencies [[cheshire "5.5.0"]
                 [clj-time "0.11.0"]
                 [com.google.appengine/appengine-api-1.0-sdk "1.9.28"]
                 [com.google.appengine/appengine-remote-api "1.9.28"]
                 [com.google.appengine/appengine-tools-sdk "1.9.28"]
                 [com.nimbusds/nimbus-jose-jwt "3.10"]
                 [me.raynes/fs "1.4.6"]
                 [org.clojure/clojure "1.7.0"]
                 [org.clojure/java.jdbc "0.5.0"]
                 [org.clojure/tools.nrepl "0.2.12"]
                 [org.postgresql/postgresql "9.4.1208"]
                 [ring/ring-core "1.3.1"]]
  :profiles {:dev {:resource-paths ["test/resources"]
                   :dependencies [[ring/ring-mock "0.3.0"]]}})
