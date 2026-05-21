package com.upb.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DireccionEnvioRequest {

    @NotNull
    private Long usuarioId;

    @NotBlank
    private String direccionCalle;

    @NotBlank
    private String ciudad;

    private String referencias;
}
