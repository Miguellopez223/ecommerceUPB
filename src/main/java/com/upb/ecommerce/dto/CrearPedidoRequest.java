package com.upb.ecommerce.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CrearPedidoRequest {

    @NotNull
    private Long tiendaId;

    @NotNull
    private Long usuarioId;

    private Long direccionId;
}
