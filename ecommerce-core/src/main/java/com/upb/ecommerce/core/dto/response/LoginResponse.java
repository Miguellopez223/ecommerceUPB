package com.upb.ecommerce.core.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * DTO de respuesta de autenticación. Sigue el patrón OAuth-style:
 * los campos JSON usan snake_case para consistencia con estándares.
 */
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse implements Serializable {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("id_token")
    private String idToken;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("token_type")
    private String tokenType;

    /** Duración del token en milisegundos. */
    @JsonProperty("expires_in")
    private long expiresIn;

    /** Timestamp Unix (ms) en que expira el token. */
    @JsonProperty("expires_at")
    private long expiresAt;
}
