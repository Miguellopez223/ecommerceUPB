package com.upb.ecommerce.core.integracion;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.UUID;

/**
 * Variante de {@link StereumService} que consume la API de Stereum manejando el JSON
 * "a mano" con {@link JSONObject} (libreria org.json), en vez de dejar que Jackson
 * convierta DTOs automaticamente.
 *
 * <p>Es el mismo ejercicio que muestra el docente en la clase {@code SistemaA.auth(...)}
 * del proyecto eventop, pero aplicado al endpoint de Stereum
 * {@code POST /api/v1/transactions/create-charge}:
 * <ol>
 *   <li>El cuerpo del request se arma campo por campo con {@code jsonObject.put(...)} y se
 *       envia como texto ({@code .body(json.toString())}).</li>
 *   <li>La respuesta se recibe como texto ({@code .toEntity(String.class)}).</li>
 *   <li>La respuesta se parsea a mano con {@code new JSONObject(body).getString("...")}.</li>
 * </ol>
 *
 * <p>El resultado es el mismo {@link StereumCreateChargeResponse} que la version tipada;
 * lo que cambia es la forma de construir/leer el JSON.
 */
@Slf4j
@Service
public class StereumServiceJson {

    @Value("${stereum.url-base}")
    private String urlBase;
    @Value("${stereum.api-key}")
    private String apiKey;
    @Value("${stereum.connect-timeout}")
    private int connectTimeout;
    @Value("${stereum.read-timeout}")
    private int readTimeout;

    public StereumCreateChargeResponse crearCargo(StereumCreateChargeRequest request) throws Exception {
        // 1) CONSTRUIR el cuerpo del request a mano con JSONObject -------------
        String idempotencyKey = (request.getIdempotencyKey() == null || request.getIdempotencyKey().isBlank())
                ? UUID.randomUUID().toString()
                : request.getIdempotencyKey();

        JSONObject customer = new JSONObject();
        if (request.getCustomer() != null) {
            StereumCustomer c = request.getCustomer();
            customer.put("name", c.getName());
            customer.put("lastname", c.getLastname());
            customer.put("document_number", c.getDocumentNumber());
            customer.put("email", c.getEmail());
            customer.put("phone", c.getPhone());
        }

        JSONObject body = new JSONObject();
        body.put("country", request.getCountry());
        body.put("amount", request.getAmount());
        body.put("currency", request.getCurrency());
        body.put("network", request.getNetwork());
        body.put("idempotency_key", idempotencyKey);
        body.put("charge_reason", request.getChargeReason());
        body.put("reservation_validity_time", request.getReservationValidityTime());
        body.put("callback", request.getCallback());
        body.put("customer", customer);

        // 2) ENVIAR como String y RECIBIR como String -------------------------
        RestClient restClient = create();
        ResponseEntity<String> response;
        try {
            response = restClient.post()
                    .uri(urlBase + "/api/v1/transactions/create-charge")
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                    .header("x-api-key", apiKey)
                    .body(body.toString())
                    .retrieve()
                    .toEntity(String.class);
        } catch (Exception e) {
            log.error("Exception al crear cargo en Stereum (JSONObject). ", e);
            throw e;
        }

        if (!response.getStatusCode().is2xxSuccessful()) {
            log.error("Se genero error: {}", response.getStatusCode().value());
            throw new Exception("Se genero error");
        }

        // 3) PARSEAR la respuesta a mano con JSONObject -----------------------
        JSONObject json = new JSONObject(response.getBody());

        StereumCreateChargeResponse result = new StereumCreateChargeResponse();
        result.setId(json.optString("id", null));
        result.setAmount(json.optBigDecimal("amount", null));
        result.setCurrency(json.optString("currency", null));
        result.setNetwork(json.optString("network", null));
        result.setQrBase64(json.optString("qr_base64", null));
        result.setPaymentLink(json.optString("payment_link", null));
        result.setTransactionStatus(json.optString("transaction_status", null));
        result.setCollectingAccount(json.optString("collecting_account", null));
        if (json.has("on_main_net")) {
            result.setOnMainNet(json.getBoolean("on_main_net"));
        }
        if (json.has("expiration_time")) {
            result.setExpirationTime(json.getLong("expiration_time"));
        }

        log.info("Cargo creado en Stereum OK (via JSONObject)");
        return result;
    }

    // --- helpers -------------------------------------------------------------

    private RestClient create() {
        SimpleClientHttpRequestFactory clientHttpRequestFactory = new SimpleClientHttpRequestFactory();
        clientHttpRequestFactory.setConnectTimeout(Duration.ofMillis(connectTimeout));
        clientHttpRequestFactory.setReadTimeout(Duration.ofMillis(readTimeout));

        return RestClient.builder().requestFactory(clientHttpRequestFactory).build();
    }
}
