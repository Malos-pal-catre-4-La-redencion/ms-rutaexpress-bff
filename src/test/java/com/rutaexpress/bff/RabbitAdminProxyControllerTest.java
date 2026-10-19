package com.rutaexpress.bff;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba solo la autorización de la ruta de administración, sin Azure AD real ni
 * RabbitMQ: el administrador apunta a un puerto cerrado. Si la autorización
 * permite el paso, el BFF intenta llamar al microservicio y responde 503
 * ("No se pudo contactar"); si la deniega, responde 401 o 403 sin llamarlo.
 */
@SpringBootTest(properties = "rutaexpress.downstream.rabbit-admin-url=http://localhost:1")
@AutoConfigureMockMvc
class RabbitAdminProxyControllerTest {

    private static final String COLA_VALIDA = "{\"nombre\":\"q.demo\"}";

    @Autowired
    private MockMvc mockMvc;

    private RequestPostProcessor conRol(String rol) {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    @Test
    void sinTokenSeRechazaConNoAutorizado() throws Exception {
        mockMvc.perform(post("/api/bff/admin/queues")
                        .contentType(MediaType.APPLICATION_JSON).content(COLA_VALIDA))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void despachadorNoPuedeCrearColas() throws Exception {
        mockMvc.perform(post("/api/bff/admin/queues").with(conRol("Despachador"))
                        .contentType(MediaType.APPLICATION_JSON).content(COLA_VALIDA))
                .andExpect(status().isForbidden());
    }

    @Test
    void clienteNoPuedeCrearColas() throws Exception {
        mockMvc.perform(post("/api/bff/admin/queues").with(conRol("Cliente"))
                        .contentType(MediaType.APPLICATION_JSON).content(COLA_VALIDA))
                .andExpect(status().isForbidden());
    }

    @Test
    void auditorNoPuedeEliminarColas() throws Exception {
        mockMvc.perform(delete("/api/bff/admin/queues/q.demo").with(conRol("Auditor")))
                .andExpect(status().isForbidden());
    }

    @Test
    void soloAdminPuedeAdministrarExchangesYBindings() throws Exception {
        mockMvc.perform(post("/api/bff/admin/exchanges").with(conRol("Despachador"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"x\",\"tipo\":\"DIRECT\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/bff/admin/bindings").with(conRol("Cliente"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exchange\":\"x\",\"cola\":\"q\",\"routingKey\":\"k\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/bff/admin/bindings").with(conRol("Despachador"))
                        .param("exchange", "x").param("cola", "q").param("routingKey", "#"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminPasaLaAutorizacionYElBffIntentaLlamarAlMicroservicio() throws Exception {
        mockMvc.perform(post("/api/bff/admin/queues").with(conRol("Admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(COLA_VALIDA))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void adminPuedeConsultarUnaColaAunqueElMicroservicioEsteCaido() throws Exception {
        mockMvc.perform(get("/api/bff/admin/queues/q.demo").with(conRol("Admin")))
                .andExpect(status().isServiceUnavailable());
    }
}
