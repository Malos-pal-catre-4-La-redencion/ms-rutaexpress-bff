package com.rutaexpress.bff.proxy;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Administración de Kafka: reenvía hacia ms-rutaexpress-kafka-admin, que no
 * valida JWT por su cuenta (mismo patrón que RabbitAdminProxyController).
 * TODA esta ruta exige rol Admin (@PreAuthorize a nivel de clase).
 *
 * Los errores del administrador (400 de validación, 404, 409, 503) se reenvían
 * tal cual mediante DownstreamExceptionHandler.
 */
@RestController
@RequestMapping("/api/bff/admin/kafka")
@PreAuthorize("hasRole('Admin')")
public class KafkaAdminProxyController {

    private final RestClient adminClient;

    public KafkaAdminProxyController(@Qualifier("kafkaAdminClient") RestClient adminClient) {
        this.adminClient = adminClient;
    }

    // ------------------------------------------------------------------ tópicos

    @PostMapping("/topics")
    public ResponseEntity<Map<String, Object>> crearTopico(@RequestBody Map<String, Object> request) {
        Map<String, Object> creado = adminClient.post().uri("/topics").body(request)
                .retrieve().body(new ParameterizedTypeReference<>() {});
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @GetMapping("/topics")
    public List<String> listarTopicos() {
        return adminClient.get().uri("/topics").retrieve().body(new ParameterizedTypeReference<>() {});
    }

    @GetMapping("/topics/{nombre}")
    public Map<String, Object> obtenerTopico(@PathVariable String nombre) {
        return adminClient.get().uri("/topics/{nombre}", nombre)
                .retrieve().body(new ParameterizedTypeReference<>() {});
    }

    @PatchMapping("/topics/{nombre}")
    public Map<String, Object> actualizarTopico(@PathVariable String nombre, @RequestBody Map<String, Object> request) {
        return adminClient.patch().uri("/topics/{nombre}", nombre).body(request)
                .retrieve().body(new ParameterizedTypeReference<>() {});
    }

    @DeleteMapping("/topics/{nombre}")
    public ResponseEntity<Void> eliminarTopico(@PathVariable String nombre) {
        adminClient.delete().uri("/topics/{nombre}", nombre).retrieve().toBodilessEntity();
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------ consumer groups

    @GetMapping("/consumer-groups")
    public List<Map<String, Object>> listarGrupos() {
        return adminClient.get().uri("/consumer-groups").retrieve().body(new ParameterizedTypeReference<>() {});
    }

    @GetMapping("/consumer-groups/{groupId}")
    public Map<String, Object> describirGrupo(@PathVariable String groupId) {
        return adminClient.get().uri("/consumer-groups/{groupId}", groupId)
                .retrieve().body(new ParameterizedTypeReference<>() {});
    }
}
