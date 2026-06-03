package com.upb.ecommerce.core.service;

import com.upb.ecommerce.core.dto.request.CrearPedidoRequest;
import com.upb.ecommerce.core.dto.request.GenerarQrRequest;
import com.upb.ecommerce.core.dto.response.PedidoResponse;
import com.upb.ecommerce.core.integracion.StereumCreateChargeRequest;
import com.upb.ecommerce.core.integracion.StereumCreateChargeResponse;
import com.upb.ecommerce.core.integracion.StereumCustomer;
import com.upb.ecommerce.core.integracion.StereumService;
import com.upb.ecommerce.data.repository.*;
import com.upb.ecommerce.domain.entities.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final CarritoRepository carritoRepository;
    private final ProductoRepository productoRepository;
    private final TiendaRepository tiendaRepository;
    private final UsuarioRepository usuarioRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final DireccionEnvioRepository direccionRepository;
    private final StereumService stereumService;

    public PedidoService(PedidoRepository pedidoRepository,
                         CarritoRepository carritoRepository,
                         ProductoRepository productoRepository,
                         TiendaRepository tiendaRepository,
                         UsuarioRepository usuarioRepository,
                         MovimientoInventarioRepository movimientoRepository,
                         DireccionEnvioRepository direccionRepository,
                         StereumService stereumService) {
        this.pedidoRepository = pedidoRepository;
        this.carritoRepository = carritoRepository;
        this.productoRepository = productoRepository;
        this.tiendaRepository = tiendaRepository;
        this.usuarioRepository = usuarioRepository;
        this.movimientoRepository = movimientoRepository;
        this.direccionRepository = direccionRepository;
        this.stereumService = stereumService;
    }

    public List<PedidoResponse> listarPorUsuario(Long tiendaId, Long usuarioId) {
        return pedidoRepository.findByUsuarioIdAndTiendaId(usuarioId, tiendaId)
                .stream().map(PedidoResponse::fromEntity).toList();
    }

    public PedidoResponse obtenerPorId(Long tiendaId, Long pedidoId) {
        return PedidoResponse.fromEntity(
                pedidoRepository.findByIdAndTiendaId(pedidoId, tiendaId)
                        .orElseThrow(() -> new RuntimeException("Pedido no encontrado")));
    }

    @Transactional
    public PedidoResponse crearDesdeCarrito(CrearPedidoRequest request) {
        Tienda tienda = tiendaRepository.findById(request.getTiendaId())
                .orElseThrow(() -> new RuntimeException("Tienda no encontrada"));
        Usuario usuario = usuarioRepository.findById(request.getUsuarioId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        Carrito carrito = carritoRepository
                .findByUsuarioIdAndTiendaIdAndEstado(request.getUsuarioId(), request.getTiendaId(), "ACTIVO")
                .orElseThrow(() -> new RuntimeException("No hay carrito activo para este usuario"));

        if (carrito.getDetalles() == null || carrito.getDetalles().isEmpty()) {
            throw new RuntimeException("El carrito está vacío");
        }

        Pedido pedido = new Pedido();
        pedido.setTienda(tienda);
        pedido.setUsuario(usuario);
        pedido.setCodigoSeguimiento("PED-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        pedido.setDetalles(new ArrayList<>());

        if (request.getDireccionId() != null) {
            DireccionEnvio direccion = direccionRepository.findById(request.getDireccionId())
                    .orElseThrow(() -> new RuntimeException("Dirección no encontrada"));
            pedido.setDireccionEnvio(direccion);
        }

        BigDecimal total = BigDecimal.ZERO;
        for (DetalleCarrito dc : carrito.getDetalles()) {
            Producto producto = dc.getProducto();
            if (producto.getStock() < dc.getCantidad()) {
                throw new RuntimeException("Stock insuficiente para: " + producto.getNombre());
            }

            producto.setStock(producto.getStock() - dc.getCantidad());
            productoRepository.save(producto);

            DetallePedido dp = new DetallePedido();
            dp.setPedido(pedido);
            dp.setProducto(producto);
            dp.setCantidad(dc.getCantidad());
            dp.setPrecioUnitario(dc.getPrecioUnitario());
            pedido.getDetalles().add(dp);

            total = total.add(dc.getPrecioUnitario().multiply(BigDecimal.valueOf(dc.getCantidad())));

            // Registro automático de SALIDA de inventario
            MovimientoInventario mov = new MovimientoInventario();
            mov.setTienda(tienda);
            mov.setProducto(producto);
            mov.setTipo("SALIDA");
            mov.setCantidad(dc.getCantidad());
            mov.setReferencia("Venta pedido #" + pedido.getCodigoSeguimiento());
            movimientoRepository.save(mov);
        }

        pedido.setTotal(total);
        pedidoRepository.save(pedido);

        carrito.setEstado("CONVERTIDO_A_PEDIDO");
        carritoRepository.save(carrito);

        return PedidoResponse.fromEntity(pedido);
    }

    @Transactional
    public PedidoResponse actualizarEstado(Long tiendaId, Long pedidoId, String nuevoEstado) {
        Pedido pedido = pedidoRepository.findByIdAndTiendaId(pedidoId, tiendaId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        List<String> estadosValidos = List.of("PENDIENTE", "PAGADO", "PREPARANDO", "ENVIADO", "ENTREGADO", "CANCELADO");
        if (!estadosValidos.contains(nuevoEstado)) {
            throw new RuntimeException("Estado no válido: " + nuevoEstado);
        }
        pedido.setEstadoPedido(nuevoEstado);
        return PedidoResponse.fromEntity(pedidoRepository.save(pedido));
    }

    /**
     * Cancela un pedido: repone el stock de cada producto, registra los movimientos
     * de ENTRADA correspondientes y deja el pedido en estado CANCELADO.
     * No se permite cancelar pedidos ya ENVIADO/ENTREGADO/CANCELADO.
     */
    @Transactional
    public PedidoResponse cancelarPedido(Long tiendaId, Long pedidoId) {
        Pedido pedido = pedidoRepository.findByIdAndTiendaId(pedidoId, tiendaId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        String estado = pedido.getEstadoPedido();
        if ("CANCELADO".equals(estado)) {
            throw new RuntimeException("El pedido ya está cancelado");
        }
        if ("ENVIADO".equals(estado) || "ENTREGADO".equals(estado)) {
            throw new RuntimeException("No se puede cancelar un pedido en estado " + estado);
        }

        if (pedido.getDetalles() != null) {
            for (DetallePedido dp : pedido.getDetalles()) {
                Producto producto = dp.getProducto();
                producto.setStock(producto.getStock() + dp.getCantidad());
                productoRepository.save(producto);

                MovimientoInventario mov = new MovimientoInventario();
                mov.setTienda(pedido.getTienda());
                mov.setProducto(producto);
                mov.setTipo("ENTRADA");
                mov.setCantidad(dp.getCantidad());
                mov.setReferencia("Cancelación pedido " + pedido.getCodigoSeguimiento());
                movimientoRepository.save(mov);
            }
        }

        pedido.setEstadoPedido("CANCELADO");
        return PedidoResponse.fromEntity(pedidoRepository.save(pedido));
    }

    /**
     * Genera un QR de pago (Stereum) para un pedido. El monto se toma del total del
     * pedido. Lanza {@code Exception} si Stereum falla (la maneja el controlador).
     */
    @Transactional(readOnly = true)
    public StereumCreateChargeResponse generarQrPago(Long tiendaId, Long pedidoId,
                                                     GenerarQrRequest req) throws Exception {
        Pedido pedido = pedidoRepository.findByIdAndTiendaId(pedidoId, tiendaId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        if ("CANCELADO".equals(pedido.getEstadoPedido())) {
            throw new RuntimeException("No se puede cobrar un pedido cancelado");
        }
        if ("PAGADO".equals(pedido.getEstadoPedido())) {
            throw new RuntimeException("El pedido ya fue pagado");
        }

        Usuario usuario = pedido.getUsuario();
        StereumCustomer customer = StereumCustomer.builder()
                .name(usuario.getNombre())
                .lastname(".")
                .documentNumber(req.getDocumentNumber() != null ? req.getDocumentNumber() : "0000000")
                .email(usuario.getEmail())
                .build();

        StereumCreateChargeRequest charge = StereumCreateChargeRequest.builder()
                .country(req.getCountry())
                .amount(pedido.getTotal().toPlainString())
                .currency(req.getCurrency())
                .network(req.getNetwork())
                .chargeReason("Pago pedido " + pedido.getCodigoSeguimiento())
                .reservationValidityTime(req.getReservationValidityTime())
                .customer(customer)
                .build();

        return stereumService.crearCargo(charge);
    }
}
