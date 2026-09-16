package com.rutaexpress.bff.proxy;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;

import java.util.Map;

/**
 * Sin esto, cualquier 404/409 que devuelva shipments o catalog llegaría al
 * frontend como un 500 genérico (la excepción de RestClient los envuelve).
 * Acá se reenvía el status y el cuerpo tal cual los mandó el microservicio.
 */
@RestControllerAdvice
public class DownstreamExceptionHandler {

    @ExceptionHandler(HttpStatusCodeException.class)
    public ResponseEntity<String> handleErrorDownstream(HttpStatusCodeException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(ex.getResponseBodyAsString());
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<Map<String, Object>> handleNoAlcanzable(ResourceAccessException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "No se pudo contactar al microservicio",
                        "detalle", ex.getMessage()
                ));
    }
}
