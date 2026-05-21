package com.upb.ecommerce.core.dto.response;

import com.upb.ecommerce.domain.entities.Carrito;
import com.upb.ecommerce.domain.entities.DetalleCarrito;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CarritoResponse {

    private Long id;
    private Long tiendaId;
    private Long usuarioId;
    private String estado;
    private BigDecimal totalEstimado;
    private List<ItemCarritoResponse> items;

    @Data
    public static class ItemCarritoResponse {
        private Long id;
        private Long productoId;
        private String productoNombre;
        private Integer cantidad;
        private BigDecimal precioUnitario;
        private BigDecimal subtotal;

        public static ItemCarritoResponse fromEntity(DetalleCarrito d) {
            ItemCarritoResponse r = new ItemCarritoResponse();
            r.setId(d.getId());
            r.setProductoId(d.getProducto().getId());
            r.setProductoNombre(d.getProducto().getNombre());
            r.setCantidad(d.getCantidad());
            r.setPrecioUnitario(d.getPrecioUnitario());
            r.setSubtotal(d.getPrecioUnitario().multiply(BigDecimal.valueOf(d.getCantidad())));
            return r;
        }
    }

    public static CarritoResponse fromEntity(Carrito c) {
        CarritoResponse r = new CarritoResponse();
        r.setId(c.getId());
        r.setTiendaId(c.getTienda().getId());
        r.setUsuarioId(c.getUsuario().getId());
        r.setEstado(c.getEstado());
        r.setTotalEstimado(c.getTotalEstimado());
        if (c.getDetalles() != null) {
            r.setItems(c.getDetalles().stream().map(ItemCarritoResponse::fromEntity).toList());
        }
        return r;
    }
}
