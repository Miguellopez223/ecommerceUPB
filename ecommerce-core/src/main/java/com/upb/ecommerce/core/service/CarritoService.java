package com.upb.ecommerce.core.service;

import com.upb.ecommerce.core.dto.request.AgregarItemCarritoRequest;
import com.upb.ecommerce.core.dto.response.CarritoResponse;
import com.upb.ecommerce.core.exception.NotDataFoundException;
import com.upb.ecommerce.core.exception.OperationException;
import com.upb.ecommerce.data.repository.CarritoRepository;
import com.upb.ecommerce.data.repository.DetalleCarritoRepository;
import com.upb.ecommerce.data.repository.ProductoRepository;
import com.upb.ecommerce.data.repository.TiendaRepository;
import com.upb.ecommerce.data.repository.UsuarioRepository;
import com.upb.ecommerce.domain.entities.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

@Slf4j
@Service
public class CarritoService {

    private final CarritoRepository carritoRepository;
    private final DetalleCarritoRepository detalleCarritoRepository;
    private final ProductoRepository productoRepository;
    private final TiendaRepository tiendaRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;

    public CarritoService(CarritoRepository carritoRepository,
                          DetalleCarritoRepository detalleCarritoRepository,
                          ProductoRepository productoRepository,
                          TiendaRepository tiendaRepository,
                          UsuarioRepository usuarioRepository,
                          EmailService emailService) {
        this.carritoRepository = carritoRepository;
        this.detalleCarritoRepository = detalleCarritoRepository;
        this.productoRepository = productoRepository;
        this.tiendaRepository = tiendaRepository;
        this.usuarioRepository = usuarioRepository;
        this.emailService = emailService;
    }

    /**
     * Procesa UN carrito abandonado en segundo plano (un hilo del pool de {@code @Async}).
     * Le envía el recordatorio por correo al cliente y lo marca como ABANDONADO.
     * Devuelve un {@link Future} con el id del carrito para que el barrido pueda esperar a
     * que todos los trabajadores de la página terminen.
     */
    @Async
    @Transactional
    public Future<Long> procesarCarritoAbandonado(Long carritoId) {
        Carrito carrito = carritoRepository.findById(carritoId).orElse(null);
        // Re-validamos dentro del hilo: pudo cambiar de estado mientras esperaba en la cola.
        if (carrito == null || !"ACTIVO".equals(carrito.getEstado())) {
            return CompletableFuture.completedFuture(carritoId);
        }

        int cantidadItems = carrito.getDetalles() == null ? 0 : carrito.getDetalles().size();
        if (cantidadItems > 0) {
            Usuario cliente = carrito.getUsuario();
            try {
                emailService.enviarRecordatorioCarrito(
                        cliente.getEmail(), cliente.getNombre(), cantidadItems, carrito.getTotalEstimado());
                log.info("Recordatorio de carrito {} enviado a {}", carritoId, cliente.getEmail());
            } catch (Exception e) {
                // Un fallo de correo no debe impedir marcar el carrito como abandonado.
                log.warn("No se pudo enviar el correo del carrito {}: {}", carritoId, e.getMessage());
            }
        }

        carrito.setEstado("ABANDONADO");
        carritoRepository.save(carrito);
        return CompletableFuture.completedFuture(carritoId);
    }

    public CarritoResponse obtenerCarritoActivo(Long tiendaId, Long usuarioId) {
        Carrito carrito = carritoRepository
                .findByUsuarioIdAndTiendaIdAndEstado(usuarioId, tiendaId, "ACTIVO")
                .orElseThrow(() -> new NotDataFoundException("No hay carrito activo para este usuario"));
        return CarritoResponse.fromEntity(carrito);
    }

    @Transactional
    public CarritoResponse agregarItem(AgregarItemCarritoRequest request) {
        Tienda tienda = tiendaRepository.findById(request.getTiendaId())
                .orElseThrow(() -> new NotDataFoundException("Tienda no encontrada"));
        Usuario usuario = usuarioRepository.findById(request.getUsuarioId())
                .orElseThrow(() -> new NotDataFoundException("Usuario no encontrado"));
        Producto producto = productoRepository.findByIdAndTiendaId(request.getProductoId(), request.getTiendaId())
                .orElseThrow(() -> new NotDataFoundException("Producto no encontrado en esta tienda"));

        if (producto.getStock() < request.getCantidad()) {
            throw new OperationException("Stock insuficiente. Disponible: " + producto.getStock());
        }

        Carrito carrito = carritoRepository
                .findByUsuarioIdAndTiendaIdAndEstado(request.getUsuarioId(), request.getTiendaId(), "ACTIVO")
                .orElseGet(() -> {
                    Carrito nuevo = new Carrito();
                    nuevo.setTienda(tienda);
                    nuevo.setUsuario(usuario);
                    nuevo.setDetalles(new ArrayList<>());
                    return carritoRepository.save(nuevo);
                });

        Optional<DetalleCarrito> detalleExistente =
                detalleCarritoRepository.findByCarritoIdAndProductoId(carrito.getId(), producto.getId());

        if (detalleExistente.isPresent()) {
            DetalleCarrito detalle = detalleExistente.get();
            detalle.setCantidad(detalle.getCantidad() + request.getCantidad());
            detalleCarritoRepository.save(detalle);
        } else {
            DetalleCarrito detalle = new DetalleCarrito();
            detalle.setCarrito(carrito);
            detalle.setProducto(producto);
            detalle.setCantidad(request.getCantidad());
            detalle.setPrecioUnitario(producto.getPrecio());
            detalleCarritoRepository.save(detalle);
        }

        recalcularTotal(carrito.getId());
        return CarritoResponse.fromEntity(carritoRepository.findById(carrito.getId()).orElseThrow());
    }

    @Transactional
    public CarritoResponse eliminarItem(Long carritoId, Long detalleId) {
        DetalleCarrito detalle = detalleCarritoRepository.findById(detalleId)
                .orElseThrow(() -> new NotDataFoundException("Item no encontrado en el carrito"));
        Long cid = detalle.getCarrito().getId();
        detalleCarritoRepository.delete(detalle);
        recalcularTotal(cid);
        return CarritoResponse.fromEntity(carritoRepository.findById(cid).orElseThrow());
    }

    @Transactional
    public CarritoResponse vaciarCarrito(Long carritoId) {
        Carrito carrito = carritoRepository.findById(carritoId)
                .orElseThrow(() -> new NotDataFoundException("Carrito no encontrado"));
        if (carrito.getDetalles() != null && !carrito.getDetalles().isEmpty()) {
            detalleCarritoRepository.deleteAll(carrito.getDetalles());
            carrito.getDetalles().clear();
        }
        carrito.setTotalEstimado(BigDecimal.ZERO);
        return CarritoResponse.fromEntity(carritoRepository.save(carrito));
    }

    private void recalcularTotal(Long carritoId) {
        Carrito fresh = carritoRepository.findById(carritoId).orElseThrow();
        BigDecimal total = BigDecimal.ZERO;
        if (fresh.getDetalles() != null) {
            for (DetalleCarrito d : fresh.getDetalles()) {
                total = total.add(d.getPrecioUnitario().multiply(BigDecimal.valueOf(d.getCantidad())));
            }
        }
        fresh.setTotalEstimado(total);
        carritoRepository.save(fresh);
    }
}
