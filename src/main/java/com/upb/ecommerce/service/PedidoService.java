package com.upb.ecommerce.service;

import com.upb.ecommerce.dto.CrearPedidoRequest;
import com.upb.ecommerce.dto.PedidoResponse;
import com.upb.ecommerce.repository.*;
import com.upb.ecommerce.repository.entities.*;
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
    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final DireccionEnvioRepository direccionEnvioRepository;

    public PedidoService(PedidoRepository pedidoRepository,
                         CarritoRepository carritoRepository,
                         ProductoRepository productoRepository,
                         TiendaRepository tiendaRepository,
                         UsuarioRepository usuarioRepository,
                         MovimientoInventarioRepository movimientoInventarioRepository,
                         DireccionEnvioRepository direccionEnvioRepository) {
        this.pedidoRepository = pedidoRepository;
        this.carritoRepository = carritoRepository;
        this.productoRepository = productoRepository;
        this.tiendaRepository = tiendaRepository;
        this.usuarioRepository = usuarioRepository;
        this.movimientoInventarioRepository = movimientoInventarioRepository;
        this.direccionEnvioRepository = direccionEnvioRepository;
    }

    public List<PedidoResponse> listarPorUsuario(Long tiendaId, Long usuarioId) {
        return pedidoRepository.findByUsuarioIdAndTiendaId(usuarioId, tiendaId)
                .stream()
                .map(PedidoResponse::fromEntity)
                .toList();
    }

    public PedidoResponse obtenerPorId(Long tiendaId, Long pedidoId) {
        Pedido pedido = pedidoRepository.findByIdAndTiendaId(pedidoId, tiendaId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
        return PedidoResponse.fromEntity(pedido);
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

        // Asignar dirección de envío si se provee
        if (request.getDireccionId() != null) {
            DireccionEnvio direccion = direccionEnvioRepository.findById(request.getDireccionId())
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

            // Registrar movimiento de inventario (SALIDA automática)
            MovimientoInventario mov = new MovimientoInventario();
            mov.setTienda(tienda);
            mov.setProducto(producto);
            mov.setTipo("SALIDA");
            mov.setCantidad(dc.getCantidad());
            mov.setReferencia("Venta pedido #" + pedido.getCodigoSeguimiento());
            movimientoInventarioRepository.save(mov);
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
}
