(ns akvo.commons.jwt-test
  (:require [clojure.test :refer :all]
            [akvo.commons.jwt :refer :all]
            [ring.mock.request :as mock])
  (import com.nimbusds.jose.jwk.RSAKey))


(def jwk-set
  "{\"keys\":[{\"kid\":\"zkbRZLbxWXhrqXAs6Lf3Gb-kQFB6LsjoPzNBuCto4-0\",\"kty\":\"RSA\",\"alg\":\"RS256\",\"use\":\"sig\",\"n\":\"hru33g0kyFxOmF7fRa5Kijv546Qe9nj_406jgcWelKuSTfJWxy79E8W8GoCNyplzTJgUVIFhUv1IN6ywOickX-iozec5Xzt2ed2lc6Sd0Urtho_4ZvGv7T2x5gEmCk02bE_CXyH8G2Jxv9wXrjd11llYXuC3C_27izyEKW0fwbT1qrG6AJb6F5jVBc4Os7mG21fmtOjmBg6jQdcOVtqlaMlP2Fgu5ZiM2UWeTXdHuRWQJ5ASg42BzdTW1WV3xu76cI-qgro3gacZa2ZdiQGyiTYTUR9dROzVL_W1uvrUJ5iss6PfZ8VYa8BRxYCmdFMNAuQilHiqtQpnSUbK2aZSsQ\",\"e\":\"AQAB\"}]}")

(def jwk
  "{\"kid\":\"zkbRZLbxWXhrqXAs6Lf3Gb-kQFB6LsjoPzNBuCto4-0\",\"kty\":\"RSA\",\"alg\":\"RS256\",\"use\":\"sig\",\"n\":\"hru33g0kyFxOmF7fRa5Kijv546Qe9nj_406jgcWelKuSTfJWxy79E8W8GoCNyplzTJgUVIFhUv1IN6ywOickX-iozec5Xzt2ed2lc6Sd0Urtho_4ZvGv7T2x5gEmCk02bE_CXyH8G2Jxv9wXrjd11llYXuC3C_27izyEKW0fwbT1qrG6AJb6F5jVBc4Os7mG21fmtOjmBg6jQdcOVtqlaMlP2Fgu5ZiM2UWeTXdHuRWQJ5ASg42BzdTW1WV3xu76cI-qgro3gacZa2ZdiQGyiTYTUR9dROzVL_W1uvrUJ5iss6PfZ8VYa8BRxYCmdFMNAuQilHiqtQpnSUbK2aZSsQ\",\"e\":\"AQAB\"}")

(deftest parse-rsa-key
  (testing "rsa parser"
    (is (instance? RSAKey (rsa-key jwk))))
  (testing "rsa set parser"
    (is (instance? RSAKey (rsa-key jwk-set 0)))))

(deftest get-jwt-token
  (is (= "abc" (jwt-token (-> (mock/request :get "https://example.org")
                              (mock/header "Authorization" "Bearer abc")))))
  (is (nil? (jwt-token (mock/request :get "https://example.org")))))
