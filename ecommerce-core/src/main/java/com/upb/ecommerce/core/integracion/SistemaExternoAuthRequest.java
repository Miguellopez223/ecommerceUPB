package com.upb.ecommerce.core.integracion;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cuerpo para autenticarse contra {@code POST /api/auth} del sistema externo.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class SistemaExternoAuthRequest {
    private String email;
    private String password;
    private Long tiendaId;
}
