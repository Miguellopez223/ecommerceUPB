package com.upb.ecommerce.core.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class MovimientoInventarioRequest {

    @NotNull
    private Long tiendaId;

    @NotNull
    private Long productoId;

    /** ID del admin que realiza el ajuste manual (opcional) */
    private Long usuarioId;

    /** "ENTRADA" o "SALIDA" */
    @NotBlank
    private String tipo;

    @NotNull
    @Positive
    private Integer cantidad;

    private String referencia;
}
