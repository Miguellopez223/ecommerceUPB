package com.upb.ecommerce.core.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CategoriaRequest {

    @NotNull
    private Long tiendaId;

    @NotBlank
    private String nombre;
}
