package com.upb.ecommerce.core.dto.response;

import com.upb.ecommerce.domain.entities.Categoria;
import lombok.Data;

@Data
public class CategoriaResponse {

    private Long id;
    private Long tiendaId;
    private String nombre;
    private Boolean estado;

    public static CategoriaResponse fromEntity(Categoria c) {
        CategoriaResponse r = new CategoriaResponse();
        r.setId(c.getId());
        r.setTiendaId(c.getTienda().getId());
        r.setNombre(c.getNombre());
        r.setEstado(c.getEstado());
        return r;
    }
}
