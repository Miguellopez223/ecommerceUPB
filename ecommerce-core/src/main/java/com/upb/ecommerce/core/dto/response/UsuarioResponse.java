package com.upb.ecommerce.core.dto.response;

import com.upb.ecommerce.domain.entities.Usuario;
import com.upb.ecommerce.domain.enums.RolType;
import lombok.Data;

@Data
public class UsuarioResponse {

    private Long id;
    private Long tiendaId;
    private String nombre;
    private String email;
    private RolType rol;
    private String numeroWhatsapp;
    private Boolean visibleCatalogo;
    private Boolean estado;

    public static UsuarioResponse fromEntity(Usuario u) {
        UsuarioResponse r = new UsuarioResponse();
        r.setId(u.getId());
        r.setTiendaId(u.getTienda().getId());
        r.setNombre(u.getNombre());
        r.setEmail(u.getEmail());
        r.setRol(u.getRol());
        r.setNumeroWhatsapp(u.getNumeroWhatsapp());
        r.setVisibleCatalogo(u.getVisibleCatalogo());
        r.setEstado(u.getEstado());
        return r;
    }
}
