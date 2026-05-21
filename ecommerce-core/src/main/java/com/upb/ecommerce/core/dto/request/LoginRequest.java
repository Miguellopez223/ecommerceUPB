package com.upb.ecommerce.core.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LoginRequest {

    @NotNull
    private Long tiendaId;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;
}
