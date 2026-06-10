package com.upb.ecommerce.core.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Punto genérico de una serie de reporte: una etiqueta (día, hora, categoría, método de
 * pago…) con su cantidad e ingresos asociados. Reutilizado por varias pestañas de reportes.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SerieItemResponse {

    private String etiqueta;
    private long cantidad;
    private BigDecimal ingresos;
}
