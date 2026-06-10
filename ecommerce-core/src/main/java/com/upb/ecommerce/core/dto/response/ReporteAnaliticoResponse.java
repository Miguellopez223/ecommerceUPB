package com.upb.ecommerce.core.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Pestaña 1 — Dashboard analítico: series de ventas/ingresos por día, por categoría y por
 * hora del día (heatmap), más la comparativa de la semana actual contra la anterior.
 */
@Data
public class ReporteAnaliticoResponse {

    private List<SerieItemResponse> ventasPorDia;
    private List<SerieItemResponse> ventasPorCategoria;
    private List<SerieItemResponse> ventasPorHora;

    private BigDecimal ingresosSemanaActual;
    private BigDecimal ingresosSemanaAnterior;
    /** Variación porcentual de los ingresos: (actual - anterior) / anterior * 100. */
    private BigDecimal variacionPorcentaje;
}
