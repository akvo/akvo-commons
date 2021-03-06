;  Copyright (C) 2016 Stichting Akvo (Akvo Foundation)
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

(ns akvo.commons.jwt
  (:require [clojure.string :as str])
  (:import java.util.Date
           java.text.ParseException
           com.nimbusds.jwt.SignedJWT
           com.nimbusds.jose.jwk.RSAKey
           com.nimbusds.jose.jwk.JWKSet
           com.nimbusds.jose.crypto.RSASSAVerifier))

(defn rsa-key
  "Parse an RSA certificate string"
  ([^String cert]
   (.toRSAPublicKey (RSAKey/parse cert)))
  ([^String cert n]
   (let [rsa-key (-> (JWKSet/parse cert)
                     .getKeys
                     (.get n))]
     (.toRSAPublicKey ^RSAKey rsa-key))))

(defn jwt-token
  "Get the jwt token from the authorization header of the request"
  [req]
  (let [auth-header (get-in req [:headers "authorization"])]
    (when (and (not (str/blank? auth-header))
               (.startsWith ^String auth-header "Bearer "))
      (subs auth-header 7))))

(defn verified-claims
  "Parses and verifies jwt token and returns the claims if the token
  is verified, otherwise nil. Throws ParseException if the token can
  not be parsed."
  [^String token verifier issuer opts]
  (let [jwt (SignedJWT/parse token)
        claims (.getJWTClaimsSet jwt)
        exp (.getExpirationTime claims)
        nbf (.getNotBeforeTime claims)
        iat (.getIssueTime claims)
        now (Date.)
        [before after] (:iat-interval opts)]
    (when (and (.verify jwt verifier)
               (= issuer (.getIssuer claims))
               (if exp (.after exp now) true)
               (if nbf (.before nbf now) true)
               (if (and iat before after)
                 (let [time (.getTime now)
                       min-iat (Date. ^Long (- time (* before 1000)))
                       max-iat (Date. ^Long (+ time (* after 1000)))]
                   (and (.after max-iat iat)
                        (.before min-iat iat)))
                 true))
      (into {} (.getAllClaims claims)))))

(defn wrap-jwt-claims
  "Verifies the jwt token using the cert string and associates the
  claims to the request if it can be verified. Takes an optional opts
  map with keys

  :iat-interval [before after] Compare the 'issued at' time (iat) with
  the interval [now - before, now + after] and consider the token
  valid only if the iat falls within this interval (in seconds)"
  ([handler rsa-key issuer]
   (wrap-jwt-claims handler rsa-key issuer {}))
  ([handler rsa-key issuer opts]
   (let [verifier (RSASSAVerifier. rsa-key)]
     (fn [req]
       (if-let [token (jwt-token req)]
         (try
           (if-let [claims (verified-claims token verifier issuer opts)]
             (handler (assoc req :jwt-claims claims))
             (handler req))
           (catch ParseException e
             (handler req)))
         (handler req))))))
