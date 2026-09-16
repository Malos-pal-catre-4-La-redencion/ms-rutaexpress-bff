package com.rutaexpress.bff.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoints puramente de verificación: cada uno exige un rol distinto para
 * comprobar, antes de escribir un solo microservicio de dominio, que la
 * cadena Azure AD -> JWT -> Spring Security funciona de punta a punta.
 */
@RestController
@RequestMapping("/api/bff")
public class PingController {

    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of("status", "ok", "mensaje", "Cualquier usuario autenticado puede ver esto");
    }

    @GetMapping("/admin/ping")
    @PreAuthorize("hasRole('Admin')")
    public Map<String, String> adminPing() {
        return Map.of("status", "ok", "mensaje", "Solo Admin puede ver esto");
    }

    @GetMapping("/despachador/ping")
    @PreAuthorize("hasAnyRole('Admin', 'Despachador')")
    public Map<String, String> despachadorPing() {
        return Map.of("status", "ok", "mensaje", "Admin o Despachador pueden ver esto");
    }

    @GetMapping("/cliente/ping")
    @PreAuthorize("hasAnyRole('Admin', 'Cliente')")
    public Map<String, String> clientePing() {
        return Map.of("status", "ok", "mensaje", "Admin o Cliente pueden ver esto");
    }

    @GetMapping("/audit/ping")
    @PreAuthorize("hasAnyRole('Admin', 'Auditor')")
    public Map<String, String> auditorPing() {
        return Map.of("status", "ok", "mensaje", "Admin o Auditor pueden ver esto (solo lectura)");
    }
}
