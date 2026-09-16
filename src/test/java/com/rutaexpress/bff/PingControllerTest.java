package com.rutaexpress.bff;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * No pega contra Azure AD real: simula un JWT ya validado, con distintos
 * roles, para probar solamente la capa de autorización (@PreAuthorize).
 */
@SpringBootTest
@AutoConfigureMockMvc
class PingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void adminPuedeEntrarAEndpointDeAdmin() throws Exception {
        mockMvc.perform(get("/api/bff/admin/ping")
                        .with(jwt().jwt(j -> j.claim("roles", java.util.List.of("Admin")))))
                .andExpect(status().isOk());
    }

    @Test
    void despachadorNoPuedeEntrarAEndpointDeAdmin() throws Exception {
        mockMvc.perform(get("/api/bff/admin/ping")
                        .with(jwt().jwt(j -> j.claim("roles", java.util.List.of("Despachador")))))
                .andExpect(status().isForbidden());
    }

    @Test
    void despachadorPuedeEntrarAEndpointDeDespachador() throws Exception {
        mockMvc.perform(get("/api/bff/despachador/ping")
                        .with(jwt().jwt(j -> j.claim("roles", java.util.List.of("Despachador")))))
                .andExpect(status().isOk());
    }

    @Test
    void sinTokenDevuelveNoAutorizado() throws Exception {
        mockMvc.perform(get("/api/bff/ping"))
                .andExpect(status().isUnauthorized());
    }
}
