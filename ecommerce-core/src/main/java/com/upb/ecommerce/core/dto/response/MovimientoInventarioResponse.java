package com.upb.ecommerce.core.dto.response;

import com.upb.ecommerce.domain.entities.MovimientoInventario;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MovimientoInventarioResponse {

    private Long id;
    private Long tiendaId;
    private Long productoId;
    private String productoNombre;
    private Long usuarioId;
    private String tipo;
    private Integer cantidad;
    private String referencia;
    private LocalDateTime fecha;

    public static MovimientoInventarioResponse fromEntity(MovimientoInventario m) {
        MovimientoInventarioResponse r = new MovimientoInventarioResponse();
        r.setId(m.getId());
        r.setTiendaId(m.getTienda().getId());
        r.setProductoId(m.getProducto().getId());
        r.setProductoNombre(m.getProducto().getNombre());
        if (m.getUsuario() != null) {
            r.setUsuarioId(m.getUsuario().getId());
        }
        r.setTipo(m.getTipo());
        r.setCantidad(m.getCantidad());
        r.setReferencia(m.getReferencia());
        r.setFecha(m.getFecha());
        return r;
    }
}
