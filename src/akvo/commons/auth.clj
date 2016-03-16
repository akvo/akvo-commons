;  Copyright (C) 2015-2016 Stichting Akvo (Akvo Foundation)
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
  (:require [akvo.commons.jwt :as jwt]
            [ring.util.response :refer (response status)])
  (:import com.nimbusds.jose.crypto.RSASSAVerifier
           com.nimbusds.jose.jwk.RSAKey))

(defn validate-token [^String token ^RSAKey rsa]
  (jwt/verified-claims token (RSASSAVerifier. (.toRSAPublicKey rsa)) {}))

(defn authorized? [req rsa]
  (let [token (jwt/jwt-token req)]
    (when (and token (validate-token token rsa))
      token)))

(defn wrap-auth [handler cert-file]
  (let [rsa (jwt/rsa-key (slurp cert-file))]
    (fn [req]
      (if-let [jwt (authorized? req rsa)]
        (handler (assoc req :jwt jwt))
        (-> (response "Access Denied")
            (status 403))))))
