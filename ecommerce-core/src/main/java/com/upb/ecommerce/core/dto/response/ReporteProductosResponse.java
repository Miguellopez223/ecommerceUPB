package com.upb.ecommerce.core.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Pestaña 3 — Productos: ranking de más vendidos, productos "muertos" (sin salidas en N
 * días), valorización del inventario y listado de stock crítico.
 *
 * <p><b>Valorización de Inventario</b> = Σ (stock disponible × precio) por producto, tanto
 * a costo como a precio de venta.
 */
@Data
public class ReporteProductosResponse {

    private List<ProductoRankingResponse> masVendidos;
    private List<ProductoResponse> productosMuertos;
    private BigDecimal valorizacionCosto;
    private BigDecimal valorizacionVenta;
    private List<ProductoResponse> stockCritico;
}
