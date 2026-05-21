package com.upb.ecommerce.core.dto.response;

import com.upb.ecommerce.domain.entities.DireccionEnvio;
import lombok.Data;

@Data
public class DireccionEnvioResponse {

    private Long id;
    private Long usuarioId;
    private String direccionCalle;
    private String ciudad;
    private String referencias;
    private Boolean estado;

    public static DireccionEnvioResponse fromEntity(DireccionEnvio d) {
        DireccionEnvioResponse r = new DireccionEnvioResponse();
        r.setId(d.getId());
        r.setUsuarioId(d.getUsuario().getId());
        r.setDireccionCalle(d.getDireccionCalle());
        r.setCiudad(d.getCiudad());
        r.setReferencias(d.getReferencias());
        r.setEstado(d.getEstado());
        return r;
    }
}
