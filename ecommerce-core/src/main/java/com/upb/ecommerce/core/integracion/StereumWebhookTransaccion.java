package com.upb.ecommerce.core.integracion;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * Objeto {@code transaction} que viaja dentro de la notificación de webhook de Stereum.
 * Refleja los campos que Stereum envía al cambiar el estado de un cargo.
 *
 * <p>{@code id} es el identificador del cargo en Stereum: es el mismo valor que se guardó
 * en {@code Pago.transaccionPasarelaId} al generar el QR, y por eso permite encontrar el
 * pago/pedido correspondiente. {@code status} es el estado nuevo (PENDIENTE, PAGADO,
 * CANCELADO, ERROR).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class StereumWebhookTransaccion {

    /** Identificador único del cargo en Stereum (= Pago.transaccionPasarelaId). */
    private String id;

    private String country;

    /** Monto por el que se generó el cargo. */
    private BigDecimal amount;

    /** Monto realmente recibido (relevante en cobros USDT/USDC). */
    @JsonProperty("amount_received")
    private BigDecimal amountReceived;

    private BigDecimal fee;

    private String currency;

    private String network;

    /** Estado nuevo del cargo: PENDIENTE, PAGADO, CANCELADO, ERROR. */
    private String status;

    @JsonProperty("status_description")
    private String statusDescription;

    @JsonProperty("on_main_net")
    private Boolean onMainNet;

    @JsonProperty("idempotency_key")
    private String idempotencyKey;

    @JsonProperty("created_date")
    private Long createdDate;
}
