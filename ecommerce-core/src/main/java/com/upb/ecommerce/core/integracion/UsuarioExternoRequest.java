package com.upb.ecommerce.core.integracion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cuerpo para crear un usuario en el sistema externo ({@code POST /api/usuarios/registrar}).
 * {@code rol} se envia como String ("ADMIN" / "CLIENTE").
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UsuarioExternoRequest {
    private Long tiendaId;
    private String nombre;
    private String email;
    private String password;
    private String rol;
}
