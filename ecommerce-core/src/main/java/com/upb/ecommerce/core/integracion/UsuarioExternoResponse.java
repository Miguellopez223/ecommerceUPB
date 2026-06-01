package com.upb.ecommerce.core.integracion;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Usuario tal como lo devuelve el sistema externo.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class UsuarioExternoResponse {
    private Long id;
    private Long tiendaId;
    private String nombre;
    private String email;
    private String rol;
    private Boolean estado;
}
