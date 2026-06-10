package com.upb.ecommerce.core.dto.request;

import com.upb.ecommerce.domain.enums.RolType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UsuarioRequest {

    @NotNull
    private Long tiendaId;

    @NotBlank
    private String nombre;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;

    /** ADMIN o CLIENTE — Jackson deserializa automáticamente desde el JSON string. */
    @NotNull
    private RolType rol;

    /** Opcional: número de WhatsApp para contacto / catálogo público. */
    private String numeroWhatsapp;

    /** Opcional: si el usuario se muestra como contacto en el catálogo público. */
    private Boolean visibleCatalogo;
}
