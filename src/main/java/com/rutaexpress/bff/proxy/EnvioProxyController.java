package com.rutaexpress.bff.proxy;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Envíos. Este controlador es el único lugar donde viven las reglas de acceso
 * (shipments no valida JWT):
 *
 *  - Admin, Despachador y Auditor ven todos los envíos.
 *  - Cualquier otro usuario autenticado (Cliente) ve solo los que él creó. Es una
 *    regla "por omisión": un rol que no esté en la lista de arriba queda restringido.
 *  - El dueño de un envío es el claim "oid" del JWT. El BFF lo envía a shipments en
 *    la cabecera X-Usuario-Id al crear, y lo usa como filtro al listar. Siempre sale
 *    del token, nunca de lo que mande el navegador.
 *  - Crear y cambiar de estado tienen restricciones por rol con @PreAuthorize.
 */
@RestController
@RequestMapping("/api/bff/envios")
public class EnvioProxyController {

    /** Cabecera con la que shipments recibe el dueño del envío. */
    static final String CABECERA_USUARIO = "X-Usuario-Id";

    /** Roles que pueden ver los envíos de todos los usuarios. */
    private static final Set<String> ROLES_QUE_VEN_TODO = Set.of("ROLE_Admin", "ROLE_Despachador", "ROLE_Auditor");

    private final RestClient shipmentsClient;

    public EnvioProxyController(@Qualifier("shipmentsClient") RestClient shipmentsClient) {
        this.shipmentsClient = shipmentsClient;
    }

    @GetMapping
    public List<Map<String, Object>> listar(Authentication authentication, @AuthenticationPrincipal Jwt jwt) {
        if (veTodosLosEnvios(authentication)) {
            return shipmentsClient.get()
                    .uri("/envios")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        }
        String usuarioId = idDelUsuario(jwt);
        return shipmentsClient.get()
                .uri(uri -> uri.path("/envios").queryParam("creadoPor", "{usuario}").build(usuarioId))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> obtener(@PathVariable Long id,
                                                       Authentication authentication,
                                                       @AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> envio = shipmentsClient.get()
                .uri("/envios/{id}", id)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        boolean esSuyo = envio != null && idDelUsuario(jwt).equals(envio.get("creadoPor"));
        if (!veTodosLosEnvios(authentication) && !esSuyo) {
            // 404 y no 403: no se revela que el envío existe y es de otra persona.
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "timestamp", Instant.now().toString(),
                    "status", HttpStatus.NOT_FOUND.value(),
                    "error", "No existe un envío con id " + id));
        }
        return ResponseEntity.ok(envio);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('Admin', 'Despachador', 'Cliente')")
    public ResponseEntity<Map<String, Object>> crear(@RequestBody Map<String, Object> request,
                                                     @AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> creado = shipmentsClient.post()
                .uri("/envios")
                .header(CABECERA_USUARIO, idDelUsuario(jwt))
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

    private static boolean veTodosLosEnvios(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(ROLES_QUE_VEN_TODO::contains);
    }

    /** "oid" identifica al usuario de forma estable en Entra ID; "sub" es el respaldo. */
    private static String idDelUsuario(Jwt jwt) {
        String oid = jwt.getClaimAsString("oid");
        return oid != null && !oid.isBlank() ? oid : jwt.getSubject();
    }
}
