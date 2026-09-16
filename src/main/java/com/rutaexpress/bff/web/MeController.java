package com.rutaexpress.bff.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Endpoint de conveniencia para el frontend: una vez logueado, Angular puede
 * pedir este endpoint para saber quién es el usuario y qué rol(es) tiene, sin
 * tener que decodificar el JWT del lado del cliente.
 */
@RestController
@RequestMapping("/api/bff")
public class MeController {

    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        return Map.of(
                "username", jwt.getClaimAsString("preferred_username"),
                "name", jwt.getClaimAsString("name"),
                "roles", roles != null ? roles : List.of(),
                "issuer", jwt.getIssuer().toString()
        );
    }
}
