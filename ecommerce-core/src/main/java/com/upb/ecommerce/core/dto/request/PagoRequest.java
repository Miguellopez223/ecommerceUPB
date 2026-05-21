package com.upb.ecommerce.core.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PagoRequest {

    @NotNull
    private Long pedidoId;

    /** "QR" o "TARJETA" */
    @NotBlank
    private String metodo;

    private String transaccionPasarelaId;

    @NotNull
    @Positive
    private BigDecimal monto;
}
