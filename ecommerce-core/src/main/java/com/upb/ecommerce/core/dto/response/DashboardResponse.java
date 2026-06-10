package com.upb.ecommerce.core.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Métricas rápidas de la tienda para el panel principal del ADMIN.
 * Equivale al "Dashboard" compartido de CompuWeb, sin el módulo de punto de venta
 * (las ventas son autoservicio del cliente con pago por QR).
 */
@Data
public class DashboardResponse {

    /** Total de productos activos. */
    private long totalProductos;

    /** Total de movimientos de inventario registrados. */
    private long totalMovimientos;

    /** Productos activos con stock = 0. */
    private long productosAgotados;

    /** Productos activos con 0 < stock <= stockMinimo (bajos pero no agotados). */
    private long productosStockBajo;

    /** Cantidad de ventas completadas de hoy. */
    private long ventasHoy;

    /** Ingresos de las ventas completadas de hoy. */
    private BigDecimal ingresosHoy;

    /** Panel de alertas: productos en estado crítico o agotado (stock <= mínimo). */
    private List<ProductoResponse> alertasStock;

    /** Las últimas 5 ventas completadas. */
    private List<PedidoResponse> ultimasVentas;
}
