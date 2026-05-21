package com.upb.ecommerce.core.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TiendaRequest {

    @NotBlank
    private String nombre;

    @NotBlank
    private String slug;

    private String telefonoContacto;
    private String emailContacto;
    private String logoUrl;
}
