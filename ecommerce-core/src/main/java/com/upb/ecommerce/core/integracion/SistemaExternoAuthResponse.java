package com.upb.ecommerce.core.integracion;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Respuesta de autenticacion del sistema externo (JSON en snake_case).
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class SistemaExternoAuthResponse {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("expires_at")
    private long expiresAt;
}
