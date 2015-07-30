;  Copyright (C) 2015 Stichting Akvo (Akvo Foundation)
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

(ns akvo.commons.auth
  (:require [clojure.string :as str]
            [ring.util.response :refer (response status)])
  (:import com.nimbusds.jwt.SignedJWT
           com.nimbusds.jose.jwk.RSAKey
           com.nimbusds.jose.crypto.RSASSAVerifier))

(defn rsa-key [cert-file]
  (RSAKey/parse (slurp cert-file)))

(defn validate-token [token rsa]
  (let [jwt (SignedJWT/parse token)
        verifier (RSASSAVerifier. (.toRSAPublicKey rsa))
        exp (if jwt (-> jwt .getJWTClaimsSet (.getExpirationTime)) nil)]
    (and
      (.verify jwt verifier)
      exp
      (.after exp (java.util.Date.)))))

(defn authorized? [req rsa]
  (let [auth-header (get-in req [:headers "authorization"])
        token (when (and (not (str/blank? auth-header))
                         (.startsWith auth-header "Bearer "))
                (subs auth-header 7))]
    (when (and token (validate-token token rsa))
      token)))

(defn wrap-auth [handler cert-file]
  (let [rsa (rsa-key cert-file)]
    (fn [req]
      (if-let [jwt (authorized? req rsa)]
        (handler (assoc req :jwt jwt))
        (-> (response "Access Denied")
            (status 403))))))
