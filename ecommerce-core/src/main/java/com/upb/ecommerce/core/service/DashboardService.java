package com.upb.ecommerce.core.service;

import com.upb.ecommerce.core.dto.response.DashboardResponse;
import com.upb.ecommerce.core.dto.response.PedidoResponse;
import com.upb.ecommerce.core.dto.response.ProductoResponse;
import com.upb.ecommerce.data.repository.MovimientoInventarioRepository;
import com.upb.ecommerce.data.repository.PedidoRepository;
import com.upb.ecommerce.data.repository.ProductoRepository;
import com.upb.ecommerce.domain.entities.Pedido;
import com.upb.ecommerce.domain.entities.Producto;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class DashboardService {

    /**
     * Estados que se consideran una "venta completada" (pedido pagado en adelante).
     * Se excluyen PENDIENTE (aún sin pagar) y CANCELADO.
     */
    static final List<String> ESTADOS_COMPLETADOS =
            List.of("PAGADO", "PREPARANDO", "ENVIADO", "ENTREGADO");

    private final ProductoRepository productoRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final PedidoRepository pedidoRepository;

    public DashboardService(ProductoRepository productoRepository,
                            MovimientoInventarioRepository movimientoRepository,
                            PedidoRepository pedidoRepository) {
        this.productoRepository = productoRepository;
        this.movimientoRepository = movimientoRepository;
        this.pedidoRepository = pedidoRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse obtener(Long tiendaId) {
        DashboardResponse dto = new DashboardResponse();

        dto.setTotalProductos(productoRepository.countByTiendaIdAndEstadoTrue(tiendaId));
        dto.setTotalMovimientos(movimientoRepository.countByTiendaId(tiendaId));

        // Alertas de stock: productos con stock <= mínimo (incluye agotados).
        List<Producto> criticos = productoRepository.findStockCritico(tiendaId);
        dto.setProductosAgotados(criticos.stream().filter(p -> p.getStock() == 0).count());
        dto.setProductosStockBajo(criticos.stream().filter(p -> p.getStock() > 0).count());
        dto.setAlertasStock(criticos.stream().map(ProductoResponse::fromEntity).toList());

        // Ventas e ingresos de hoy.
        LocalDateTime inicioHoy = LocalDate.now().atStartOfDay();
        LocalDateTime finHoy = LocalDate.now().atTime(LocalTime.MAX);
        List<Pedido> ventasHoy = pedidoRepository.findVentasEntre(
                tiendaId, ESTADOS_COMPLETADOS, inicioHoy, finHoy);
        dto.setVentasHoy(ventasHoy.size());
        dto.setIngresosHoy(ventasHoy.stream()
                .map(Pedido::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        // Últimas 5 ventas completadas.
        List<Pedido> ultimas = pedidoRepository.findUltimasVentas(
                tiendaId, ESTADOS_COMPLETADOS, PageRequest.of(0, 5));
        dto.setUltimasVentas(ultimas.stream().map(PedidoResponse::fromEntity).toList());

        return dto;
    }
}
