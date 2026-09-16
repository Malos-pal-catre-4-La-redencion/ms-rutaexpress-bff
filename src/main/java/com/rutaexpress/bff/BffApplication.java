package com.rutaexpress.bff;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class BffApplication {

    private static final Logger log = LoggerFactory.getLogger(BffApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(BffApplication.class, args);
    }

    @Bean
    CommandLineRunner startupBanner(
            @Value("${server.port}") String port,
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
            @Value("${rutaexpress.cors.allowed-origin}") String allowedOrigin,
            @Value("${rutaexpress.downstream.shipments-url}") String shipmentsUrl,
            @Value("${rutaexpress.downstream.catalog-url}") String catalogUrl) {
        return args -> {
            log.info("🚀 ms-rutaexpress-bff arriba en el puerto {}", port);
            log.info("🔐 Validando JWT contra issuer: {}", issuerUri);
            log.info("🌐 CORS habilitado para: {}", allowedOrigin);
            log.info("🚚 Reenviando envíos hacia: {}", shipmentsUrl);
            log.info("🗂️ Reenviando catálogo hacia: {}", catalogUrl);
            log.info("✅ Endpoints: /api/bff/me · /api/bff/envios · /api/bff/servicios · /api/bff/vehiculos "
                    + "(+ /api/bff/ping y variantes por rol)");
        };
    }
}
