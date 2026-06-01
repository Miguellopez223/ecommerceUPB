package com.upb.ecommerce.core.integracion;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * Respuesta de Stereum al generar un QR de cobro. {@code qrBase64} es la imagen del QR
 * en base64 (lista para mostrar como {@code data:image/jpeg;base64,...}).
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class StereumCreateChargeResponse {

    private String id;
    private BigDecimal amount;
    private String currency;
    private String network;

    @JsonProperty("qr_base64")
    private String qrBase64;

    @JsonProperty("payment_link")
    private String paymentLink;

    @JsonProperty("transaction_status")
    private String transactionStatus;

    @JsonProperty("collecting_account")
    private String collectingAccount;

    @JsonProperty("on_main_net")
    private Boolean onMainNet;

    @JsonProperty("expiration_time")
    private Long expirationTime;
}
