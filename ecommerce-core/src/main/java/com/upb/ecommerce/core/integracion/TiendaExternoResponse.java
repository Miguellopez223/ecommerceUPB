package com.upb.ecommerce.core.integracion;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Tienda tal como la devuelve el sistema externo.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class TiendaExternoResponse {
    private Long id;
    private String nombre;
    private String slug;
    private String telefonoContacto;
    private String emailContacto;
    private String logoUrl;
    private Boolean estado;
}
