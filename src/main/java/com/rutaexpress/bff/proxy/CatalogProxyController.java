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

@RestController
@RequestMapping("/api/bff")
public class CatalogProxyController {

    private final RestClient catalogClient;

    public CatalogProxyController(@Qualifier("catalogClient") RestClient catalogClient) {
        this.catalogClient = catalogClient;
    }

    // ---- Servicios de envío: cualquiera ve el catálogo, solo Admin lo edita ----

    @GetMapping("/servicios")
    public List<Map<String, Object>> listarServicios() {
        return catalogClient.get().uri("/servicios").retrieve().body(new ParameterizedTypeReference<>() {});
    }

    @GetMapping("/servicios/{id}")
    public Map<String, Object> obtenerServicio(@PathVariable Long id) {
        return catalogClient.get().uri("/servicios/{id}", id).retrieve().body(new ParameterizedTypeReference<>() {});
    }

    @PostMapping("/servicios")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<Map<String, Object>> crearServicio(@RequestBody Map<String, Object> request) {
        Map<String, Object> creado = catalogClient.post().uri("/servicios").body(request)
                .retrieve().body(new ParameterizedTypeReference<>() {});
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PutMapping("/servicios/{id}")
    @PreAuthorize("hasRole('Admin')")
    public Map<String, Object> actualizarServicio(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        return catalogClient.put().uri("/servicios/{id}", id).body(request)
                .retrieve().body(new ParameterizedTypeReference<>() {});
    }

    @DeleteMapping("/servicios/{id}")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<Void> desactivarServicio(@PathVariable Long id) {
        catalogClient.delete().uri("/servicios/{id}", id).retrieve().toBodilessEntity();
        return ResponseEntity.noContent().build();
    }

    // ---- Vehículos: Admin y Despachador administran la flota ----

    @GetMapping("/vehiculos")
    @PreAuthorize("hasAnyRole('Admin', 'Despachador')")
    public List<Map<String, Object>> listarVehiculos() {
        return catalogClient.get().uri("/vehiculos").retrieve().body(new ParameterizedTypeReference<>() {});
    }

    @PostMapping("/vehiculos")
    @PreAuthorize("hasAnyRole('Admin', 'Despachador')")
    public ResponseEntity<Map<String, Object>> crearVehiculo(@RequestBody Map<String, Object> request) {
        Map<String, Object> creado = catalogClient.post().uri("/vehiculos").body(request)
                .retrieve().body(new ParameterizedTypeReference<>() {});
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PatchMapping("/vehiculos/{id}/disponibilidad")
    @PreAuthorize("hasAnyRole('Admin', 'Despachador')")
    public Map<String, Object> cambiarDisponibilidad(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        return catalogClient.patch().uri("/vehiculos/{id}/disponibilidad", id).body(request)
                .retrieve().body(new ParameterizedTypeReference<>() {});
    }
}
