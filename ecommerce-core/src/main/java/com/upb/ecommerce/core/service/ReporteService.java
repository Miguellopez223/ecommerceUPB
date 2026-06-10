package com.upb.ecommerce.core.service;

import com.upb.ecommerce.core.dto.response.*;
import com.upb.ecommerce.data.repository.MovimientoInventarioRepository;
import com.upb.ecommerce.data.repository.PagoRepository;
import com.upb.ecommerce.data.repository.PedidoRepository;
import com.upb.ecommerce.data.repository.ProductoRepository;
import com.upb.ecommerce.domain.entities.DetallePedido;
import com.upb.ecommerce.domain.entities.Pago;
import com.upb.ecommerce.domain.entities.Pedido;
import com.upb.ecommerce.domain.entities.Producto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reportes analíticos del ADMIN (5 pestañas). Todas las agregaciones se calculan en memoria
 * a partir de pedidos completados traídos con fetch-join, evitando JPQL dependiente del
 * dialecto y problemas de lazy-loading.
 *
 * <p>Una "venta completada" es un pedido en estado PAGADO/PREPARANDO/ENVIADO/ENTREGADO
 * (ver {@link DashboardService#ESTADOS_COMPLETADOS}).
 */
@Service
public class ReporteService {

    private static final List<String> ESTADOS = DashboardService.ESTADOS_COMPLETADOS;

    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;
    private final PagoRepository pagoRepository;
    private final MovimientoInventarioRepository movimientoRepository;

    public ReporteService(PedidoRepository pedidoRepository,
                          ProductoRepository productoRepository,
                          PagoRepository pagoRepository,
                          MovimientoInventarioRepository movimientoRepository) {
        this.pedidoRepository = pedidoRepository;
        this.productoRepository = productoRepository;
        this.pagoRepository = pagoRepository;
        this.movimientoRepository = movimientoRepository;
    }

    // ── Pestaña 1: Dashboard analítico ───────────────────────────────────────────

    @Transactional(readOnly = true)
    public ReporteAnaliticoResponse analitico(Long tiendaId, LocalDate desde, LocalDate hasta) {
        LocalDateTime ini = inicio(desde);
        LocalDateTime fin = fin(hasta);
        List<Pedido> ventas = pedidoRepository.findCompletadasConDetalle(tiendaId, ESTADOS, ini, fin);

        ReporteAnaliticoResponse dto = new ReporteAnaliticoResponse();
        dto.setVentasPorDia(agruparPorDia(ventas));
        dto.setVentasPorCategoria(agruparPorCategoria(ventas));
        dto.setVentasPorHora(agruparPorHora(ventas));

        // Comparativa semana actual vs. anterior (lunes a hoy).
        LocalDateTime inicioActual = LocalDate.now().with(DayOfWeek.MONDAY).atStartOfDay();
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime inicioAnterior = inicioActual.minusWeeks(1);
        LocalDateTime finAnterior = inicioActual.minusSeconds(1);

        BigDecimal actual = pedidoRepository.sumIngresos(tiendaId, ESTADOS, inicioActual, ahora);
        BigDecimal anterior = pedidoRepository.sumIngresos(tiendaId, ESTADOS, inicioAnterior, finAnterior);
        dto.setIngresosSemanaActual(nz(actual));
        dto.setIngresosSemanaAnterior(nz(anterior));
        dto.setVariacionPorcentaje(variacion(nz(actual), nz(anterior)));
        return dto;
    }

    private List<SerieItemResponse> agruparPorDia(List<Pedido> ventas) {
        Map<LocalDate, long[]> conteo = new LinkedHashMap<>();   // [cantidad]
        Map<LocalDate, BigDecimal> ingresos = new LinkedHashMap<>();
        ventas.stream()
                .filter(p -> p.getFechaCreacion() != null)
                .sorted(Comparator.comparing(Pedido::getFechaCreacion))
                .forEach(p -> {
                    LocalDate dia = p.getFechaCreacion().toLocalDate();
                    conteo.computeIfAbsent(dia, k -> new long[1])[0]++;
                    ingresos.merge(dia, nz(p.getTotal()), BigDecimal::add);
                });
        List<SerieItemResponse> serie = new ArrayList<>();
        conteo.forEach((dia, c) -> serie.add(
                new SerieItemResponse(dia.toString(), c[0], ingresos.get(dia))));
        return serie;
    }

    private List<SerieItemResponse> agruparPorHora(List<Pedido> ventas) {
        long[] cantidad = new long[24];
        BigDecimal[] ingresos = new BigDecimal[24];
        for (int i = 0; i < 24; i++) ingresos[i] = BigDecimal.ZERO;
        for (Pedido p : ventas) {
            if (p.getFechaCreacion() == null) continue;
            int h = p.getFechaCreacion().getHour();
            cantidad[h]++;
            ingresos[h] = ingresos[h].add(nz(p.getTotal()));
        }
        List<SerieItemResponse> serie = new ArrayList<>(24);
        for (int h = 0; h < 24; h++) {
            serie.add(new SerieItemResponse(String.format("%02d", h), cantidad[h], ingresos[h]));
        }
        return serie;
    }

    private List<SerieItemResponse> agruparPorCategoria(List<Pedido> ventas) {
        Map<String, long[]> cantidad = new LinkedHashMap<>();
        Map<String, BigDecimal> ingresos = new LinkedHashMap<>();
        for (Pedido p : ventas) {
            for (DetallePedido d : detalles(p)) {
                Producto pr = d.getProducto();
                String cat = (pr != null && pr.getCategoria() != null)
                        ? pr.getCategoria().getNombre() : "Sin categoría";
                cantidad.computeIfAbsent(cat, k -> new long[1])[0] += d.getCantidad();
                ingresos.merge(cat, subtotal(d), BigDecimal::add);
            }
        }
        List<SerieItemResponse> serie = new ArrayList<>();
        cantidad.forEach((cat, c) -> serie.add(
                new SerieItemResponse(cat, c[0], ingresos.get(cat))));
        serie.sort(Comparator.comparing(SerieItemResponse::getIngresos).reversed());
        return serie;
    }

    // ── Pestaña 2: Ventas ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ReporteVentasResponse ventas(Long tiendaId, LocalDate desde, LocalDate hasta) {
        LocalDateTime ini = inicio(desde);
        LocalDateTime fin = fin(hasta);
        List<Pedido> ventas = pedidoRepository.findCompletadasConDetalle(tiendaId, ESTADOS, ini, fin);

        BigDecimal ingresosBrutos = BigDecimal.ZERO;
        BigDecimal costoVentas = BigDecimal.ZERO;
        for (Pedido p : ventas) {
            ingresosBrutos = ingresosBrutos.add(nz(p.getTotal()));
            for (DetallePedido d : detalles(p)) {
                BigDecimal costoUnit = d.getProducto() != null ? nz(d.getProducto().getPrecioCosto()) : BigDecimal.ZERO;
                costoVentas = costoVentas.add(costoUnit.multiply(BigDecimal.valueOf(d.getCantidad())));
            }
        }
        BigDecimal utilidad = ingresosBrutos.subtract(costoVentas);
        long total = ventas.size();

        ReporteVentasResponse dto = new ReporteVentasResponse();
        dto.setTotalVentas(total);
        dto.setIngresosBrutos(ingresosBrutos);
        dto.setCostoVentas(costoVentas);
        dto.setUtilidadBruta(utilidad);
        dto.setTicketPromedio(total == 0 ? BigDecimal.ZERO
                : ingresosBrutos.divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP));
        dto.setMargenPorcentaje(porcentaje(utilidad, ingresosBrutos));

        // Desglose por método de pago (pagos exitosos del rango).
        Map<String, long[]> cantidad = new LinkedHashMap<>();
        Map<String, BigDecimal> montos = new LinkedHashMap<>();
        for (Pago pago : pagoRepository.findExitososEntre(tiendaId, ini, fin)) {
            String metodo = pago.getMetodo() != null ? pago.getMetodo() : "DESCONOCIDO";
            cantidad.computeIfAbsent(metodo, k -> new long[1])[0]++;
            montos.merge(metodo, nz(pago.getMonto()), BigDecimal::add);
        }
        List<SerieItemResponse> porMetodo = new ArrayList<>();
        cantidad.forEach((m, c) -> porMetodo.add(new SerieItemResponse(m, c[0], montos.get(m))));
        dto.setPorMetodoPago(porMetodo);
        return dto;
    }

    // ── Pestaña 3: Productos ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ReporteProductosResponse productos(Long tiendaId, LocalDate desde, LocalDate hasta,
                                              int diasSinMovimiento) {
        LocalDateTime ini = inicio(desde);
        LocalDateTime fin = fin(hasta);
        List<Pedido> ventas = pedidoRepository.findCompletadasConDetalle(tiendaId, ESTADOS, ini, fin);

        // Ranking de más vendidos (top 10) en el rango.
        Map<Long, long[]> unidades = new LinkedHashMap<>();
        Map<Long, BigDecimal> ingresos = new LinkedHashMap<>();
        Map<Long, String> nombres = new LinkedHashMap<>();
        for (Pedido p : ventas) {
            for (DetallePedido d : detalles(p)) {
                if (d.getProducto() == null) continue;
                Long id = d.getProducto().getId();
                nombres.putIfAbsent(id, d.getProducto().getNombre());
                unidades.computeIfAbsent(id, k -> new long[1])[0] += d.getCantidad();
                ingresos.merge(id, subtotal(d), BigDecimal::add);
            }
        }
        List<ProductoRankingResponse> masVendidos = new ArrayList<>();
        unidades.forEach((id, u) -> masVendidos.add(
                new ProductoRankingResponse(id, nombres.get(id), u[0], ingresos.get(id))));
        masVendidos.sort(Comparator.comparingLong(ProductoRankingResponse::getCantidadVendida).reversed());

        ReporteProductosResponse dto = new ReporteProductosResponse();
        dto.setMasVendidos(masVendidos.size() > 10 ? masVendidos.subList(0, 10) : masVendidos);

        // Valorización del inventario y stock crítico (punto en el tiempo, no por rango).
        List<Producto> activos = productoRepository.findByTiendaIdAndEstadoTrue(tiendaId);
        BigDecimal valCosto = BigDecimal.ZERO;
        BigDecimal valVenta = BigDecimal.ZERO;
        for (Producto p : activos) {
            BigDecimal stock = BigDecimal.valueOf(p.getStock());
            valCosto = valCosto.add(nz(p.getPrecioCosto()).multiply(stock));
            valVenta = valVenta.add(nz(p.getPrecio()).multiply(stock));
        }
        dto.setValorizacionCosto(valCosto);
        dto.setValorizacionVenta(valVenta);
        dto.setStockCritico(productoRepository.findStockCritico(tiendaId)
                .stream().map(ProductoResponse::fromEntity).toList());

        // Productos "muertos": sin SALIDA en los últimos N días (o nunca).
        Map<Long, LocalDateTime> ultimaSalida = new LinkedHashMap<>();
        for (Object[] fila : movimientoRepository.ultimaSalidaPorProducto(tiendaId)) {
            ultimaSalida.put((Long) fila[0], (LocalDateTime) fila[1]);
        }
        LocalDateTime limite = LocalDateTime.now().minusDays(diasSinMovimiento);
        List<ProductoResponse> muertos = new ArrayList<>();
        for (Producto p : activos) {
            LocalDateTime ult = ultimaSalida.get(p.getId());
            if (ult == null || ult.isBefore(limite)) {
                muertos.add(ProductoResponse.fromEntity(p));
            }
        }
        dto.setProductosMuertos(muertos);
        return dto;
    }

    // ── Pestaña 4: Clientes (reemplaza "Vendedores") ─────────────────────────────

    @Transactional(readOnly = true)
    public List<ReporteClienteResponse> clientes(Long tiendaId, LocalDate desde, LocalDate hasta) {
        List<Pedido> ventas = pedidoRepository.findCompletadasConDetalle(
                tiendaId, ESTADOS, inicio(desde), fin(hasta));

        Map<Long, long[]> compras = new LinkedHashMap<>();
        Map<Long, BigDecimal> gastado = new LinkedHashMap<>();
        Map<Long, String[]> datos = new LinkedHashMap<>();  // [nombre, email]
        for (Pedido p : ventas) {
            if (p.getUsuario() == null) continue;
            Long id = p.getUsuario().getId();
            datos.putIfAbsent(id, new String[]{p.getUsuario().getNombre(), p.getUsuario().getEmail()});
            compras.computeIfAbsent(id, k -> new long[1])[0]++;
            gastado.merge(id, nz(p.getTotal()), BigDecimal::add);
        }
        List<ReporteClienteResponse> ranking = new ArrayList<>();
        compras.forEach((id, c) -> ranking.add(new ReporteClienteResponse(
                id, datos.get(id)[0], datos.get(id)[1], c[0], gastado.get(id))));
        ranking.sort(Comparator.comparing(ReporteClienteResponse::getTotalGastado).reversed());
        return ranking;
    }

    // ── Pestaña 5: Movimientos ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<MovimientoInventarioResponse> movimientos(Long tiendaId, String tipo, Long usuarioId,
                                                          LocalDate desde, LocalDate hasta) {
        LocalDateTime ini = desde != null ? desde.atStartOfDay() : null;
        LocalDateTime fin = hasta != null ? hasta.atTime(LocalTime.MAX) : null;
        String tipoNorm = (tipo != null && !tipo.isBlank()) ? tipo.toUpperCase() : null;
        return movimientoRepository.filtrar(tiendaId, tipoNorm, usuarioId, ini, fin)
                .stream().map(MovimientoInventarioResponse::fromEntity).toList();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    /** Rango por defecto: últimos 30 días si no se especifica 'desde'. */
    private LocalDateTime inicio(LocalDate desde) {
        return (desde != null ? desde : LocalDate.now().minusDays(30)).atStartOfDay();
    }

    private LocalDateTime fin(LocalDate hasta) {
        return (hasta != null ? hasta : LocalDate.now()).atTime(LocalTime.MAX);
    }

    private List<DetallePedido> detalles(Pedido p) {
        return p.getDetalles() != null ? p.getDetalles() : List.of();
    }

    private BigDecimal subtotal(DetallePedido d) {
        return nz(d.getPrecioUnitario()).multiply(BigDecimal.valueOf(d.getCantidad()));
    }

    private BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    /** Porcentaje parte/total con 2 decimales (0 si el total es 0). */
    private BigDecimal porcentaje(BigDecimal parte, BigDecimal total) {
        if (total.signum() == 0) return BigDecimal.ZERO;
        return parte.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP);
    }

    /** Variación % entre dos períodos; si el anterior es 0 devuelve 100 (o 0 si ambos 0). */
    private BigDecimal variacion(BigDecimal actual, BigDecimal anterior) {
        if (anterior.signum() == 0) {
            return actual.signum() == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(100);
        }
        return actual.subtract(anterior)
                .multiply(BigDecimal.valueOf(100))
                .divide(anterior, 2, RoundingMode.HALF_UP);
    }
}
