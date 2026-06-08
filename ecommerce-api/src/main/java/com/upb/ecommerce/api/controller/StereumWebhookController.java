package com.upb.ecommerce.api.controller;

import tools.jackson.databind.ObjectMapper;
import com.upb.ecommerce.core.integracion.StereumWebhookNotificacion;
import com.upb.ecommerce.core.integracion.StereumWebhookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Recibe las notificaciones de webhook que Stereum Pay envía cuando cambia el estado de
 * un cargo (Transacción). Es un endpoint <b>público</b> (Stereum no envía JWT); la
 * seguridad se basa en la firma HMAC-SHA256 que Stereum incluye en cada petición.
 *
 * <p>Flujo en cada POST:
 * <ol>
 *   <li>Recalcular la firma del cuerpo con nuestro API Key y compararla con la cabecera
 *       {@code x-signature}. Si no coincide → 403.</li>
 *   <li>Verificar que la notificación no sea demasiado vieja usando {@code x-timestamp}
 *       (anti-repetición). Si pasó el margen → 403.</li>
 *   <li>Convertir el JSON a objeto.</li>
 *   <li>Si es de tipo "test" (validación de la URL), responder 200 sin procesar.</li>
 *   <li>Si es de tipo "transaction", actualizar el pago/pedido correspondiente.</li>
 * </ol>
 */
@Slf4j
@RestController
@RequestMapping("/api/webhooks/stereum")
public class StereumWebhookController {

    /** Margen máximo de antigüedad de la notificación (anti-replay). */
    private static final long TOLERANCIA_SEGUNDOS = 5 * 60; // 5 minutos

    private final StereumWebhookService stereumWebhookService;
    private final ObjectMapper objectMapper;

    /** Mismo API Key que se usa para llamar a Stereum; es la llave secreta del HMAC. */
    @Value("${stereum.api-key}")
    private String apiKey;

    public StereumWebhookController(StereumWebhookService stereumWebhookService,
                                    ObjectMapper objectMapper) {
        this.stereumWebhookService = stereumWebhookService;
        this.objectMapper = objectMapper;
    }

    @PostMapping(value = "/outbound", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> outbound(
            @RequestHeader("x-signature") String signature,
            @RequestHeader("x-timestamp") long timestamp,
            @RequestBody String body) {

        // 1. Verificar la firma: recalculamos el HMAC del cuerpo crudo con nuestro API Key.
        verificarFirma(body, signature);

        // 2. Verificar que la notificación sea reciente (anti-repetición).
        verificarTiempo(timestamp);

        // 3. Convertir el JSON (texto) a objeto.
        StereumWebhookNotificacion notificacion = parsear(body);

        // 4. Notificación de prueba (validación de la URL en la consola): solo responder 200.
        if ("test".equals(notificacion.getNotificationType())) {
            log.info("Webhook de prueba recibido y validado");
            return ResponseEntity.ok().build();
        }

        // 5. Notificación real de cambio de estado de un cargo.
        if ("transaction".equals(notificacion.getNotificationType())) {
            stereumWebhookService.procesarNotificacion(notificacion);
            return ResponseEntity.ok().build();
        }

        log.warn("Tipo de notificación no soportado: {}", notificacion.getNotificationType());
        return ResponseEntity.ok().build();
    }

    // --- helpers -------------------------------------------------------------

    private void verificarFirma(String body, String signature) {
        String firmaEsperada = new HmacUtils(HmacAlgorithms.HMAC_SHA_256,
                apiKey.getBytes(StandardCharsets.UTF_8))
                .hmacHex(body.getBytes(StandardCharsets.UTF_8));

        if (!firmaEsperada.equalsIgnoreCase(signature)) {
            log.warn("Firma de webhook inválida — se rechaza la petición");
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Firma inválida");
        }
    }

    private void verificarTiempo(long timestamp) {
        long ahora = Instant.now().getEpochSecond();
        if (Math.abs(ahora - timestamp) > TOLERANCIA_SEGUNDOS) {
            log.warn("Webhook fuera del margen de tiempo (timestamp={}, ahora={}) — se rechaza",
                    timestamp, ahora);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Notificación expirada");
        }
    }

    private StereumWebhookNotificacion parsear(String body) {
        try {
            return objectMapper.readValue(body, StereumWebhookNotificacion.class);
        } catch (Exception e) {
            log.error("No se pudo parsear el cuerpo del webhook", e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "JSON inválido");
        }
    }
}
