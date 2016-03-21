(ns akvo.commons.jwt-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer :all]
            [akvo.commons.jwt :refer :all]
            [ring.mock.request :as mock])
  (:import [java.util Date]
           [java.text ParseException]
           [java.security KeyPairGenerator]
           [java.security.interfaces RSAPublicKey]
           [com.nimbusds.jose JWSHeader JWSAlgorithm]
           [com.nimbusds.jose.jwk RSAKey]
           [com.nimbusds.jose.crypto RSASSASigner RSASSAVerifier]
           [com.nimbusds.jwt JWTClaimsSet SignedJWT]))

(deftest test-parse-rsa-key
  (testing "rsa parser"
    (is (instance? RSAPublicKey (rsa-key (slurp (io/resource "jwk.json"))))))
  (testing "rsa set parser"
    (is (instance? RSAPublicKey (rsa-key (slurp (io/resource "jwk-set.json"))
                                         0)))))

(deftest test-get-jwt-token
  (let [token (slurp (io/resource "token"))]
    (is (= token (jwt-token (-> (mock/request :get "https://example.org")
                                (mock/header "Authorization" (str "Bearer " token)))))))
  (is (nil? (jwt-token (mock/request :get "https://example.org")))))

(defn rsa-key-pair []
  (let [key-gen (KeyPairGenerator/getInstance "RSA")]
    (.initialize key-gen 1024)
    (.genKeyPair key-gen)))

(defn signer [key-pair]
  (RSASSASigner. (.getPrivate key-pair)))

(defn signed-jwt [signer subject issuer {:keys [exp nbf iat]}]
  (let [claims-set (doto (JWTClaimsSet.)
                     (.setSubject subject)
                     (.setIssuer issuer))]
    (when exp (.setExpirationTime claims-set exp))
    (when nbf (.setNotBeforeTime claims-set nbf))
    (when iat (.setIssueTime claims-set iat))
    (let [jwt (SignedJWT. (JWSHeader. JWSAlgorithm/RS256)
                                 claims-set)]
      (.sign jwt signer)
      (.serialize jwt))))

(defn date [offset]
  (Date. (+ (.getTime (Date.)) (* offset 1000))))

(def issuer "http://example-issuer.org")

(deftest test-verified-claims
  (let [key-pair (rsa-key-pair)
        token (signed-jwt (signer key-pair)
                          "alice"
                          issuer
                          {:exp (date 10)
                           :nbf (date -1)
                           :iat (date 0)})
        expired-token (signed-jwt (signer key-pair)
                                  "alice"
                                  issuer
                                  {:exp (date -10)})
        too-soon-token (signed-jwt (signer key-pair)
                                   "alice"
                                   issuer
                                   {:nbf (date 10)})

        bad-token (signed-jwt (signer (rsa-key-pair))
                              "alice"
                              issuer
                              {})
        invalid-token "foo"
        verifier (RSASSAVerifier. (.getPublic key-pair))]
    (is (map? (verified-claims token verifier issuer {})))
    (is (map? (verified-claims token verifier issuer {:iat-interval [10 10]})))
    (is (nil? (verified-claims token verifier issuer {:iat-interval [-5 10]})))
    (is (nil? (verified-claims token verifier issuer {:iat-interval [10 -5]})))
    (is (nil? (verified-claims bad-token verifier issuer {})))
    (is (thrown? ParseException (verified-claims invalid-token verifier issuer {})))
    (is (nil? (verified-claims expired-token verifier issuer {})))
    (is (nil? (verified-claims too-soon-token verifier issuer {})))))

(deftest test-wrap-jwt-claims
  (let [key-pair (rsa-key-pair)
        token (signed-jwt (signer key-pair)
                          "alice"
                          issuer
                          {})
        bad-token (signed-jwt (signer (rsa-key-pair))
                              "alice"
                              issuer
                              {})
        invalid-token "foo"
        request (fn [token]
                  (-> (mock/request :get "https://example.org")
                      (mock/header "Authorization" (str "Bearer " token))))
        handler (wrap-jwt-claims :jwt-claims
                                 (.getPublic key-pair)
                                 issuer)]

    (is (map? (handler (request token))))
    (is (nil? (handler (request bad-token))))
    (is (nil? (handler (request invalid-token))))))
