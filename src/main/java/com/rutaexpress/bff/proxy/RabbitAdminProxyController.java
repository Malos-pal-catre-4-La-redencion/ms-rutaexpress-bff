package com.rutaexpress.bff.proxy;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Administración de RabbitMQ: reenvía hacia ms-rutaexpress-rabbit-admin, que no
 * valida JWT por su cuenta. La regla vive acá y solo acá: TODA esta ruta exige
 * rol Admin (@PreAuthorize a nivel de clase). Cualquier otro rol recibe 403 sin
 * que la llamada llegue al microservicio.
 *
 * Los errores del administrador (400 de validación, 404, 409, 503) se reenvían
 * tal cual mediante DownstreamExceptionHandler.
 */
@RestController
@RequestMapping("/api/bff/admin")
@PreAuthorize("hasRole('Admin')")
public class RabbitAdminProxyController {

    private final RestClient adminClient;

    public RabbitAdminProxyController(@Qualifier("rabbitAdminClient") RestClient adminClient) {
        this.adminClient = adminClient;
    }

    // ------------------------------------------------------------------ colas

    @PostMapping("/queues")
    public ResponseEntity<Map<String, Object>> crearCola(@RequestBody Map<String, Object> request) {
        Map<String, Object> creada = adminClient.post().uri("/queues").body(request)
                .retrieve().body(new ParameterizedTypeReference<>() {});
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @GetMapping("/queues/{name}")
    public Map<String, Object> obtenerCola(@PathVariable("name") String name) {
        return adminClient.get().uri("/queues/{name}", name)
                .retrieve().body(new ParameterizedTypeReference<>() {});
    }

    @DeleteMapping("/queues/{name}")
    public ResponseEntity<Void> eliminarCola(@PathVariable("name") String name) {
        adminClient.delete().uri("/queues/{name}", name).retrieve().toBodilessEntity();
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------- exchanges

    @PostMapping("/exchanges")
    public ResponseEntity<Map<String, Object>> crearExchange(@RequestBody Map<String, Object> request) {
        Map<String, Object> creado = adminClient.post().uri("/exchanges").body(request)
                .retrieve().body(new ParameterizedTypeReference<>() {});
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @DeleteMapping("/exchanges/{name}")
    public ResponseEntity<Void> eliminarExchange(@PathVariable("name") String name) {
        adminClient.delete().uri("/exchanges/{name}", name).retrieve().toBodilessEntity();
        return ResponseEntity.noContent().build();
    }

    // --------------------------------------------------------------- bindings

    @PostMapping("/bindings")
    public ResponseEntity<Map<String, Object>> crearBinding(@RequestBody Map<String, Object> request) {
        Map<String, Object> creado = adminClient.post().uri("/bindings").body(request)
                .retrieve().body(new ParameterizedTypeReference<>() {});
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    /** La routing key puede llevar # o *, por eso viaja como variable de URI (se codifica sola). */
    @DeleteMapping("/bindings")
    public ResponseEntity<Void> eliminarBinding(@RequestParam("exchange") String exchange,
                                                @RequestParam("cola") String cola,
                                                @RequestParam("routingKey") String routingKey) {
        adminClient.delete()
                .uri("/bindings?exchange={exchange}&cola={cola}&routingKey={routingKey}", exchange, cola, routingKey)
                .retrieve().toBodilessEntity();
        return ResponseEntity.noContent().build();
    }
}
