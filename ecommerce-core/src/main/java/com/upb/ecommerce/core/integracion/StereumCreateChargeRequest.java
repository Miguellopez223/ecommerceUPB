package com.upb.ecommerce.core.integracion;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cuerpo para generar un QR de cobro en Stereum
 * ({@code POST /api/v1/transactions/create-charge}).
 *
 * <p>Los campos en snake_case usan {@code @JsonProperty} para serializar igual que
 * la documentacion. {@code idempotency_key} debe ser un UUID v4 unico por cobro;
 * si se deja null, {@code StereumService} lo genera automaticamente.
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class StereumCreateChargeRequest {

    /** Codigo ISO del pais desde donde se invoca el API. Ej: "BO". */
    private String country;

    /** Monto a cobrar como string, max 2 decimales con separador ".". */
    private String amount;

    /** Moneda: "USDT" / "USDC" (POLYGON) o "BOB" (CSL). */
    private String currency;

    /** Red o banco para recaudar. Ej: "POLYGON". */
    private String network;

    @JsonProperty("idempotency_key")
    private String idempotencyKey;

    @JsonProperty("charge_reason")
    private String chargeReason;

    /** Validez del QR en minutos (default 10). */
    @JsonProperty("reservation_validity_time")
    private String reservationValidityTime;

    /** URL opcional de redireccion tras pago via payment_link. */
    private String callback;

    private StereumCustomer customer;
}
