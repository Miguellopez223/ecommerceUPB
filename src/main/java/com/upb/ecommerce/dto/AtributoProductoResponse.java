package com.upb.ecommerce.dto;

import com.upb.ecommerce.repository.entities.AtributoProducto;
import lombok.Data;

@Data
public class AtributoProductoResponse {

    private Long id;
    private Long productoId;
    private String nombre;
    private String valor;

    public static AtributoProductoResponse fromEntity(AtributoProducto a) {
        AtributoProductoResponse r = new AtributoProductoResponse();
        r.setId(a.getId());
        r.setProductoId(a.getProducto().getId());
        r.setNombre(a.getNombre());
        r.setValor(a.getValor());
        return r;
    }
}
