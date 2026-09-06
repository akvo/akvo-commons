(defproject org.akvo/commons "0.4.9"
  :description "Akvo utility library"
  :url "http://akvo.org"
  :license {:name "GNU Affero General Public License v3.0"
            :url "https://www.gnu.org/licenses/agpl-3.0.html"}
  :signing {:gpg-key "devops@akvo.org"}
  :deploy-repositories [["releases" :clojars]
                        ["snapshots" :clojars]]
  :dependencies [[cheshire "5.5.0"]
                 [clj-time "0.11.0"]
                 ;; 2.0.38 rather than 1.9.28: second-generation App Engine descriptors
                 ;; carry <runtime> and <app-engine-apis>, which 1.9.x rejects outright,
                 ;; and this library exists to read those descriptors. 2.0.38 is the last
                 ;; release built for Java 8. The three artifacts move together because
                 ;; tools-sdk and api-1.0-sdk share ~2300 repackaged classes, so mixing
                 ;; versions leaves which copy wins to classpath order.
                 [com.google.appengine/appengine-api-1.0-sdk "2.0.38"]
                 [com.google.appengine/appengine-remote-api "2.0.38"]
                 [com.google.appengine/appengine-tools-sdk "2.0.38"]
                 [com.nimbusds/nimbus-jose-jwt "3.10"]
                 [me.raynes/fs "1.4.6"]
                 [org.clojure/clojure "1.7.0"]
                 [org.clojure/java.jdbc "0.5.0"]
                 [org.clojure/tools.nrepl "0.2.12"]
                 [org.postgresql/postgresql "9.4.1208"]
                 [ring/ring-core "1.3.1"]]
  :profiles {:dev {:resource-paths ["test/resources"]
                   :dependencies [[ring/ring-mock "0.3.0"]]}})
