(ns akvo.commons.jwt-test
  (:require [clojure.test :refer :all]
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

(def jwk-set
  "{\"keys\":[{\"kid\":\"zkbRZLbxWXhrqXAs6Lf3Gb-kQFB6LsjoPzNBuCto4-0\",\"kty\":\"RSA\",\"alg\":\"RS256\",\"use\":\"sig\",\"n\":\"hru33g0kyFxOmF7fRa5Kijv546Qe9nj_406jgcWelKuSTfJWxy79E8W8GoCNyplzTJgUVIFhUv1IN6ywOickX-iozec5Xzt2ed2lc6Sd0Urtho_4ZvGv7T2x5gEmCk02bE_CXyH8G2Jxv9wXrjd11llYXuC3C_27izyEKW0fwbT1qrG6AJb6F5jVBc4Os7mG21fmtOjmBg6jQdcOVtqlaMlP2Fgu5ZiM2UWeTXdHuRWQJ5ASg42BzdTW1WV3xu76cI-qgro3gacZa2ZdiQGyiTYTUR9dROzVL_W1uvrUJ5iss6PfZ8VYa8BRxYCmdFMNAuQilHiqtQpnSUbK2aZSsQ\",\"e\":\"AQAB\"}]}")

(def jwk
  "{\"kid\":\"zkbRZLbxWXhrqXAs6Lf3Gb-kQFB6LsjoPzNBuCto4-0\",\"kty\":\"RSA\",\"alg\":\"RS256\",\"use\":\"sig\",\"n\":\"hru33g0kyFxOmF7fRa5Kijv546Qe9nj_406jgcWelKuSTfJWxy79E8W8GoCNyplzTJgUVIFhUv1IN6ywOickX-iozec5Xzt2ed2lc6Sd0Urtho_4ZvGv7T2x5gEmCk02bE_CXyH8G2Jxv9wXrjd11llYXuC3C_27izyEKW0fwbT1qrG6AJb6F5jVBc4Os7mG21fmtOjmBg6jQdcOVtqlaMlP2Fgu5ZiM2UWeTXdHuRWQJ5ASg42BzdTW1WV3xu76cI-qgro3gacZa2ZdiQGyiTYTUR9dROzVL_W1uvrUJ5iss6PfZ8VYa8BRxYCmdFMNAuQilHiqtQpnSUbK2aZSsQ\",\"e\":\"AQAB\"}")

(deftest test-parse-rsa-key
  (testing "rsa parser"
    (is (instance? RSAPublicKey (rsa-key jwk))))
  (testing "rsa set parser"
    (is (instance? RSAPublicKey (rsa-key jwk-set 0)))))

(deftest test-get-jwt-token
  (is (= "abc" (jwt-token (-> (mock/request :get "https://example.org")
                              (mock/header "Authorization" "Bearer abc")))))
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

(deftest test-verified-claims
  (let [key-pair (rsa-key-pair)
        token (signed-jwt (signer key-pair)
                          "alice"
                          "http://example.org"
                          {:exp (date 10)
                           :nbf (date -1)
                           :iat (date 0)})
        expired-token (signed-jwt (signer key-pair)
                                  "alice"
                                  "http://example.org"
                                  {:exp (date -10)})
        too-soon-token (signed-jwt (signer key-pair)
                                   "alice"
                                   "http://example.org"
                                   {:nbf (date 10)})

        bad-token (signed-jwt (signer (rsa-key-pair))
                              "alice"
                              "http://example.org"
                              {})
        invalid-token "foo"
        verifier (RSASSAVerifier. (.getPublic key-pair))]
    (is (map? (verified-claims token verifier {})))
    (is (map? (verified-claims token verifier {:iat-interval [10 10]})))
    (is (nil? (verified-claims token verifier {:iat-interval [-5 10]})))
    (is (nil? (verified-claims token verifier {:iat-interval [10 -5]})))
    (is (nil? (verified-claims bad-token verifier {})))
    (is (thrown? ParseException (verified-claims invalid-token verifier {})))
    (is (nil? (verified-claims expired-token verifier {})))
    (is (nil? (verified-claims too-soon-token verifier {})))))

(deftest test-wrap-jwt-claims
  (let [key-pair (rsa-key-pair)
        token (signed-jwt (signer key-pair)
                          "alice"
                          "http://example.org"
                          {})
        bad-token (signed-jwt (signer (rsa-key-pair))
                              "alice"
                              "http://example.org"
                              {})
        invalid-token "foo"
        request (fn [token]
                  (-> (mock/request :get "https://example.org")
                       (mock/header "Authorization" (str "Bearer " token))))
        handler (wrap-jwt-claims :jwt-claims
                                 (.getPublic key-pair))]

    (is (map? (handler (request token))))
    (is (nil? (handler (request bad-token))))
    (is (nil? (handler (request invalid-token))))))

;; (run-tests)
