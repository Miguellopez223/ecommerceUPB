package com.upb.ecommerce.core.dto.response;

import com.upb.ecommerce.domain.entities.DetallePedido;
import com.upb.ecommerce.domain.entities.Pedido;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PedidoResponse {

    private Long id;
    private Long tiendaId;
    private Long usuarioId;
    private String codigoSeguimiento;
    private BigDecimal total;
    private String estadoPedido;
    private Long direccionId;
    private List<ItemPedidoResponse> items;

    @Data
    public static class ItemPedidoResponse {
        private Long id;
        private Long productoId;
        private String productoNombre;
        private Integer cantidad;
        private BigDecimal precioUnitario;
        private BigDecimal subtotal;

        public static ItemPedidoResponse fromEntity(DetallePedido d) {
            ItemPedidoResponse r = new ItemPedidoResponse();
            r.setId(d.getId());
            r.setProductoId(d.getProducto().getId());
            r.setProductoNombre(d.getProducto().getNombre());
            r.setCantidad(d.getCantidad());
            r.setPrecioUnitario(d.getPrecioUnitario());
            r.setSubtotal(d.getPrecioUnitario().multiply(BigDecimal.valueOf(d.getCantidad())));
            return r;
        }
    }

    public static PedidoResponse fromEntity(Pedido p) {
        PedidoResponse r = new PedidoResponse();
        r.setId(p.getId());
        r.setTiendaId(p.getTienda().getId());
        r.setUsuarioId(p.getUsuario().getId());
        r.setCodigoSeguimiento(p.getCodigoSeguimiento());
        r.setTotal(p.getTotal());
        r.setEstadoPedido(p.getEstadoPedido());
        if (p.getDireccionEnvio() != null) {
            r.setDireccionId(p.getDireccionEnvio().getId());
        }
        if (p.getDetalles() != null) {
            r.setItems(p.getDetalles().stream().map(ItemPedidoResponse::fromEntity).toList());
        }
        return r;
    }
}
