package com.rutaexpress.bff.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * El BFF nunca expone estas URLs al frontend — Angular solo conoce
 * /api/bff/*. Estos clientes son de uso interno, para reenviar hacia los
 * microservicios de dominio, que no validan JWT por su cuenta.
 */
@Configuration
public class DownstreamConfig {

    @Value("${rutaexpress.downstream.shipments-url}")
    private String shipmentsUrl;

    @Value("${rutaexpress.downstream.catalog-url}")
    private String catalogUrl;

    @Bean
    public RestClient shipmentsClient() {
        return RestClient.builder().baseUrl(shipmentsUrl).build();
    }

    @Bean
    public RestClient catalogClient() {
        return RestClient.builder().baseUrl(catalogUrl).build();
    }
}
