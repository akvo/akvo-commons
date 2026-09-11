(defproject org.akvo/commons "0.4.9"
  :description "Akvo utility library"
  :url "http://akvo.org"
  :license {:name "GNU Affero General Public License v3.0"
            :url "https://www.gnu.org/licenses/agpl-3.0.html"}
  ;; Unsigned, and deliberately so. Releases up to 0.4.8 were signed with a
  ;; devops@akvo.org key that now lives only on whichever machine last cut a
  ;; release, which is what left 0.4.9 unpublished while the code that needed it
  ;; was already merged. Signing from CI would mean putting that private key in
  ;; repository secrets, readable by anyone who can push a workflow here.
  ;;
  ;; Clojars does not require signatures, and org.akvo.flow/akvo-flow has always
  ;; published without them from the same CI, so this matches what the
  ;; organisation already does rather than inventing a weaker practice for one
  ;; library. Removing `:signing` with it -- leaving the key configured while
  ;; disabling its only use reads as an oversight.
  ;; Credentials are named per repository. Leiningen does not read a general
  ;; LEIN_USERNAME/LEIN_PASSWORD pair for a repository that declares none: it
  ;; finds no credentials, falls through to an interactive prompt, reads EOF on a
  ;; CI runner and uploads empty ones. The 401 that follows says "authentication
  ;; failed", which reads like a bad token rather than a missing declaration.
  ;;
  ;; `:env/clojars_username` names the environment variable CLOJARS_USERNAME, so
  ;; these match the secrets in the workflow exactly.
  :deploy-repositories [["releases" {:url "https://repo.clojars.org"
                                     :username :env/clojars_username
                                     :password :env/clojars_password
                                     :sign-releases false}]
                        ["snapshots" {:url "https://repo.clojars.org"
                                      :username :env/clojars_username
                                      :password :env/clojars_password
                                      :sign-releases false}]]
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
