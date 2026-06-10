package com.upb.ecommerce.core.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Fila del ranking de clientes por compras. Reemplaza al "ranking de vendedores" de
 * CompuWeb: como aquí las ventas son autoservicio del cliente, se rankea por cliente.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReporteClienteResponse {

    private Long usuarioId;
    private String nombre;
    private String email;
    private long cantidadCompras;
    private BigDecimal totalGastado;
}
