package com.upb.ecommerce.core.integracion;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.UUID;

/**
 * Consume la API de pagos Stereum. A diferencia del ecommerce par, Stereum NO usa
 * login/token: la autenticacion es un header fijo {@code x-api-key} con el API Key ID.
 *
 * <p>Mismo patron estructural que {@code SistemaA} del proyecto eventop: un
 * {@link RestClient} creado con timeouts en {@link #create()}, llamadas que verifican
 * el status y lanzan {@code Exception} ante error.
 */
@Slf4j
@Service
public class StereumService {

    @Value("${stereum.url-base}")
    private String urlBase;
    @Value("${stereum.api-key}")
    private String apiKey;
    @Value("${stereum.connect-timeout}")
    private int connectTimeout;
    @Value("${stereum.read-timeout}")
    private int readTimeout;

    /**
     * Genera un QR de cobro: {@code POST /api/v1/transactions/create-charge}.
     * Si el request no trae {@code idempotency_key}, se genera un UUID v4 nuevo.
     */
    public StereumCreateChargeResponse crearCargo(StereumCreateChargeRequest request) throws Exception {
        if (request.getIdempotencyKey() == null || request.getIdempotencyKey().isBlank()) {
            request.setIdempotencyKey(UUID.randomUUID().toString());
        }

        RestClient restClient = create();

        ResponseEntity<StereumCreateChargeResponse> response;
        try {
            response = restClient.post()
                    .uri(urlBase + "/api/v1/transactions/create-charge")
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                    .header("x-api-key", apiKey)
                    .body(request)
                    .retrieve()
                    .toEntity(StereumCreateChargeResponse.class);
        } catch (Exception e) {
            log.error("Exception al crear cargo en Stereum. ", e);
            throw e;
        }

        if (!response.getStatusCode().is2xxSuccessful()) {
            log.error("Se genero error: {}", response.getStatusCode().value());
            throw new Exception("Se genero error");
        }

        log.info("Cargo creado en Stereum OK");
        return response.getBody();
    }

    // --- helpers -------------------------------------------------------------

    private RestClient create() {
        SimpleClientHttpRequestFactory clientHttpRequestFactory = new SimpleClientHttpRequestFactory();
        clientHttpRequestFactory.setConnectTimeout(Duration.ofMillis(connectTimeout));
        clientHttpRequestFactory.setReadTimeout(Duration.ofMillis(readTimeout));

        return RestClient.builder().requestFactory(clientHttpRequestFactory).build();
    }
}
