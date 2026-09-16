package com.rutaexpress.bff.proxy;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Cualquier usuario autenticado puede ver envíos (ya lo exige la regla
 * general de SecurityConfig: .anyRequest().authenticated()). Crear y
 * cambiar de estado sí tienen restricciones adicionales, aplicadas acá con
 * @PreAuthorize — este es el único lugar del sistema donde vive esa regla,
 * shipments no la repite.
 */
@RestController
@RequestMapping("/api/bff/envios")
public class EnvioProxyController {

    private final RestClient shipmentsClient;

    public EnvioProxyController(@Qualifier("shipmentsClient") RestClient shipmentsClient) {
        this.shipmentsClient = shipmentsClient;
    }

    @GetMapping
    public List<Map<String, Object>> listar() {
        return shipmentsClient.get()
                .uri("/envios")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    @GetMapping("/{id}")
    public Map<String, Object> obtener(@PathVariable Long id) {
        return shipmentsClient.get()
                .uri("/envios/{id}", id)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('Admin', 'Despachador', 'Cliente')")
    public ResponseEntity<Map<String, Object>> crear(@RequestBody Map<String, Object> request) {
        Map<String, Object> creado = shipmentsClient.post()
                .uri("/envios")
                .body(request)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('Admin', 'Despachador')")
    public Map<String, Object> cambiarEstado(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        return shipmentsClient.patch()
                .uri("/envios/{id}/estado", id)
                .body(request)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}
