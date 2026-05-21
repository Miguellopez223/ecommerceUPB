package com.upb.ecommerce.core.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AtributoProductoRequest {

    @NotNull
    private Long productoId;

    @NotBlank
    private String nombre;

    @NotBlank
    private String valor;
}
