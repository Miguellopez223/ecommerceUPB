package com.upb.ecommerce.core.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Pestaña 2 — Ventas: indicadores financieros del período y desglose por método de pago.
 *
 * <ul>
 *   <li><b>Utilidad Bruta</b> = Σ (precio de venta − precio de costo) por ítem vendido.</li>
 *   <li><b>Margen %</b> = Utilidad Bruta / Ingresos Brutos × 100.</li>
 * </ul>
 */
@Data
public class ReporteVentasResponse {

    private long totalVentas;
    private BigDecimal ingresosBrutos;
    private BigDecimal costoVentas;
    private BigDecimal utilidadBruta;
    private BigDecimal ticketPromedio;
    private BigDecimal margenPorcentaje;

    /** Desglose por método de pago (etiqueta = método, cantidad = nº pagos, ingresos = monto). */
    private List<SerieItemResponse> porMetodoPago;
}
