package com.upb.ecommerce.core.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UnidadMedidaRequest {

    @NotNull
    private Long tiendaId;

    @NotBlank
    private String nombre;

    private String abreviatura;
}
