package com.upb.ecommerce.core.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class AgregarItemCarritoRequest {

    @NotNull
    private Long tiendaId;

    @NotNull
    private Long usuarioId;

    @NotNull
    private Long productoId;

    @NotNull
    @Positive
    private Integer cantidad;
}
