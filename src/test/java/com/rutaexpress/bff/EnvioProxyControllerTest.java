package com.rutaexpress.bff;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba la regla "el Cliente solo ve sus envíos" sin Azure AD ni shipments reales:
 * shipments se reemplaza por un servidor HTTP mínimo que anota lo que el BFF le pide.
 */
@SpringBootTest
@AutoConfigureMockMvc
class EnvioProxyControllerTest {

    private static final AtomicReference<String> consultaRecibida = new AtomicReference<>();
    private static final AtomicReference<String> cabeceraUsuarioRecibida = new AtomicReference<>();
    private static final HttpServer SHIPMENTS_FALSO = iniciarShipmentsFalso();

    @DynamicPropertySource
    static void apuntarAlShipmentsFalso(DynamicPropertyRegistry registry) {
        registry.add("rutaexpress.downstream.shipments-url",
                () -> "http://localhost:" + SHIPMENTS_FALSO.getAddress().getPort());
    }

    @AfterAll
    static void detenerShipmentsFalso() {
        SHIPMENTS_FALSO.stop(0);
    }

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void limpiar() {
        consultaRecibida.set(null);
        cabeceraUsuarioRecibida.set(null);
    }

    private static RequestPostProcessor usuario(String oid, String... roles) {
        List<GrantedAuthority> autoridades = Arrays.stream(roles)
                .<GrantedAuthority>map(rol -> new SimpleGrantedAuthority("ROLE_" + rol))
                .toList();
        return jwt()
                .jwt(j -> j.claim("oid", oid))
                .authorities(autoridades);
    }

    // ---- Listar ----

    @Test
    void clienteListaSoloSusEnvios() throws Exception {
        mockMvc.perform(get("/api/bff/envios").with(usuario("oid-ana", "Cliente")))
                .andExpect(status().isOk());

        assertThat(consultaRecibida.get()).isEqualTo("creadoPor=oid-ana");
    }

    @Test
    void clienteNoPuedeSaltarseElFiltroPasandoOtroUsuarioPorParametro() throws Exception {
        mockMvc.perform(get("/api/bff/envios").param("creadoPor", "oid-luis")
                        .with(usuario("oid-ana", "Cliente")))
                .andExpect(status().isOk());

        assertThat(consultaRecibida.get()).isEqualTo("creadoPor=oid-ana");
    }

    @Test
    void adminDespachadorYAuditorVenTodosLosEnvios() throws Exception {
        for (String rol : new String[]{"Admin", "Despachador", "Auditor"}) {
            consultaRecibida.set("pendiente");

            mockMvc.perform(get("/api/bff/envios").with(usuario("oid-" + rol, rol)))
                    .andExpect(status().isOk());

            assertThat(consultaRecibida.get()).as("consulta de " + rol).isNull();
        }
    }

    @Test
    void unRolDesconocidoQuedaRestringidoASusPropiosEnvios() throws Exception {
        mockMvc.perform(get("/api/bff/envios").with(usuario("oid-sin-rol", "Visitante")))
                .andExpect(status().isOk());

        assertThat(consultaRecibida.get()).isEqualTo("creadoPor=oid-sin-rol");
    }

    @Test
    void siUnUsuarioTieneClienteYAdminPrevaleceAdmin() throws Exception {
        mockMvc.perform(get("/api/bff/envios").with(usuario("oid-ana", "Cliente", "Admin")))
                .andExpect(status().isOk());

        assertThat(consultaRecibida.get()).isNull();
    }

    @Test
    void sinTokenSeRechazaConNoAutorizado() throws Exception {
        mockMvc.perform(get("/api/bff/envios")).andExpect(status().isUnauthorized());
    }

    // ---- Abrir un envío ----

    @Test
    void clienteAbreUnEnvioPropio() throws Exception {
        mockMvc.perform(get("/api/bff/envios/1").with(usuario("oid-ana", "Cliente")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creadoPor").value("oid-ana"));
    }

    @Test
    void clienteNoPuedeAbrirUnEnvioAjenoYRecibe404() throws Exception {
        mockMvc.perform(get("/api/bff/envios/2").with(usuario("oid-ana", "Cliente")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No existe un envío con id 2"));
    }

    @Test
    void clienteNoPuedeAbrirUnEnvioSinDueno() throws Exception {
        mockMvc.perform(get("/api/bff/envios/3").with(usuario("oid-ana", "Cliente")))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminPuedeAbrirUnEnvioAjeno() throws Exception {
        mockMvc.perform(get("/api/bff/envios/2").with(usuario("oid-admin", "Admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creadoPor").value("oid-luis"));
    }

    // ---- Crear ----

    private static final String ENVIO_NUEVO =
            "{\"remitente\":\"Tienda\",\"destinatario\":\"Ana\",\"direccionDestino\":\"Calle 1\",\"pesoKg\":10}";

    @Test
    void alCrearElBffInformaAShipmentsQuienEsElUsuarioSegunElToken() throws Exception {
        mockMvc.perform(post("/api/bff/envios").with(usuario("oid-ana", "Cliente"))
                        .contentType(MediaType.APPLICATION_JSON).content(ENVIO_NUEVO))
                .andExpect(status().isCreated());

        assertThat(cabeceraUsuarioRecibida.get()).isEqualTo("oid-ana");
    }

    @Test
    void laCabeceraQueMandaElNavegadorNoSeReenvia() throws Exception {
        mockMvc.perform(post("/api/bff/envios").with(usuario("oid-ana", "Cliente"))
                        .header("X-Usuario-Id", "oid-falso")
                        .contentType(MediaType.APPLICATION_JSON).content(ENVIO_NUEVO))
                .andExpect(status().isCreated());

        assertThat(cabeceraUsuarioRecibida.get()).isEqualTo("oid-ana");
    }

    // ---- Servidor falso de shipments ----

    private static HttpServer iniciarShipmentsFalso() {
        try {
            HttpServer servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            servidor.createContext("/envios", intercambio -> {
                String ruta = intercambio.getRequestURI().getPath();
                consultaRecibida.set(intercambio.getRequestURI().getRawQuery());
                cabeceraUsuarioRecibida.set(intercambio.getRequestHeaders().getFirst("X-Usuario-Id"));
                intercambio.getRequestBody().readAllBytes();

                int codigo = 200;
                String cuerpo;
                if ("POST".equals(intercambio.getRequestMethod())) {
                    codigo = 201;
                    cuerpo = "{\"id\":9,\"creadoPor\":\"" + cabeceraUsuarioRecibida.get() + "\"}";
                } else if (ruta.equals("/envios/1")) {
                    cuerpo = "{\"id\":1,\"creadoPor\":\"oid-ana\"}";
                } else if (ruta.equals("/envios/2")) {
                    cuerpo = "{\"id\":2,\"creadoPor\":\"oid-luis\"}";
                } else if (ruta.equals("/envios/3")) {
                    cuerpo = "{\"id\":3,\"creadoPor\":null}";
                } else {
                    cuerpo = "[{\"id\":1,\"creadoPor\":\"oid-ana\"}]";
                }

                byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
                intercambio.getResponseHeaders().add("Content-Type", "application/json");
                intercambio.sendResponseHeaders(codigo, bytes.length);
                intercambio.getResponseBody().write(bytes);
                intercambio.close();
            });
            servidor.start();
            return servidor;
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo iniciar el shipments falso", ex);
        }
    }
}
