package com.upb.ecommerce.core.integracion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cuerpo para crear una tienda en el sistema externo ({@code POST /api/tiendas}).
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class TiendaExternoRequest {
    private String nombre;
    private String slug;
    private String telefonoContacto;
    private String emailContacto;
    private String logoUrl;
}
