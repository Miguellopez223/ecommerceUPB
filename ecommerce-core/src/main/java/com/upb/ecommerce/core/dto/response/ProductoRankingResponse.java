package com.upb.ecommerce.core.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Fila del ranking de productos más vendidos. */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductoRankingResponse {

    private Long productoId;
    private String nombre;
    private long cantidadVendida;
    private BigDecimal ingresos;
}
