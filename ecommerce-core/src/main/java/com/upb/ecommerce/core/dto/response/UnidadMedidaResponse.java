package com.upb.ecommerce.core.dto.response;

import com.upb.ecommerce.domain.entities.UnidadMedida;
import lombok.Data;

@Data
public class UnidadMedidaResponse {

    private Long id;
    private Long tiendaId;
    private String nombre;
    private String abreviatura;
    private Boolean estado;

    public static UnidadMedidaResponse fromEntity(UnidadMedida u) {
        UnidadMedidaResponse r = new UnidadMedidaResponse();
        r.setId(u.getId());
        r.setTiendaId(u.getTienda().getId());
        r.setNombre(u.getNombre());
        r.setAbreviatura(u.getAbreviatura());
        r.setEstado(u.getEstado());
        return r;
    }
}
