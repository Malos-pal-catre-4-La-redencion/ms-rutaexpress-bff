package com.rutaexpress.bff.config;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Spring Security no valida el claim "aud" por defecto cuando el resourceAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
 * server se configura solo con issuer-uri — solo revisa firma, issuer y
 * expiración. Este validador cierra ese hueco: rechaza cualquier JWT cuyo
 * audience no sea exactamente el de esta API (por ejemplo, un id_token, que
 * trae como aud el client id pelado en vez de "api://<clientId>").
 */
public class AudienceValidator implements OAuth2TokenValidator<Jwt> {

    private final String expectedAudience;

    public AudienceValidator(String expectedAudience) {
        this.expectedAudience = expectedAudience;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        if (jwt.getAudience().contains(expectedAudience)) {
            return OAuth2TokenValidatorResult.success();
        }
        OAuth2Error error = new OAuth2Error(
                "invalid_token",
                "El audience del token no corresponde a la API de RutaExpress (¿enviaste un id_token en vez de un access_token?)",
                null);
        return OAuth2TokenValidatorResult.failure(error);
    }
}
