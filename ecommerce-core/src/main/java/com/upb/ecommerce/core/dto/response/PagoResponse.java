package com.upb.ecommerce.core.dto.response;

import com.upb.ecommerce.domain.entities.Pago;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PagoResponse {

    private Long id;
    private Long pedidoId;
    private String metodo;
    private String transaccionPasarelaId;
    private BigDecimal monto;
    private String estadoPago;

    public static PagoResponse fromEntity(Pago p) {
        PagoResponse r = new PagoResponse();
        r.setId(p.getId());
        r.setPedidoId(p.getPedido().getId());
        r.setMetodo(p.getMetodo());
        r.setTransaccionPasarelaId(p.getTransaccionPasarelaId());
        r.setMonto(p.getMonto());
        r.setEstadoPago(p.getEstadoPago());
        return r;
    }
}
