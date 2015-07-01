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
    [akvo.commons.config :as config]
    [ring.util.response :refer (response status)])
  (:import com.nimbusds.jwt.SignedJWT
           com.nimbusds.jose.jwk.RSAKey
           com.nimbusds.jose.crypto.RSASSAVerifier))

(def rsa (RSAKey/parse (slurp (:certs config/settings))))

(defn validate-token [token]
  (let [jwt (SignedJWT/parse token)
        verifier (RSASSAVerifier. (.toRSAPublicKey rsa))
        exp (if jwt (-> jwt .getJWTClaimsSet (.getExpirationTime)) nil)]
    (and
      (.verify jwt verifier)
      exp
      (.after exp (java.util.Date.)))))

(defn authorized? [req]
  (let [auth-header (get-in req [:headers "authorization"])
        token (if (and (not (str/blank? auth-header))
                       (.startsWith auth-header "Bearer "))
                (.substring auth-header 7))]
    (if (and token (validate-token token))
      token
      nil)))

(defn wrap-auth [handler]
  (fn [req]
    (if-let [jwt (authorized? req)]
      (handler (assoc req :jwt jwt))
      (-> (response "Access Denied")
          (status 403)))))
