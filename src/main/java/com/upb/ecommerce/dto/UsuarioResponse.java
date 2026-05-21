package com.upb.ecommerce.dto;

import com.upb.ecommerce.repository.entities.Usuario;
import lombok.Data;

@Data
public class UsuarioResponse {

    private Long id;
    private Long tiendaId;
    private String nombre;
    private String email;
    private String rol;
    private Boolean estado;

    public static UsuarioResponse fromEntity(Usuario u) {
        UsuarioResponse r = new UsuarioResponse();
        r.setId(u.getId());
        r.setTiendaId(u.getTienda().getId());
        r.setNombre(u.getNombre());
        r.setEmail(u.getEmail());
        r.setRol(u.getRol());
        r.setEstado(u.getEstado());
        return r;
    }
}
