package com.upb.ecommerce.service;

import com.upb.ecommerce.dto.AgregarItemCarritoRequest;
import com.upb.ecommerce.dto.CarritoResponse;
import com.upb.ecommerce.repository.CarritoRepository;
import com.upb.ecommerce.repository.DetalleCarritoRepository;
import com.upb.ecommerce.repository.ProductoRepository;
import com.upb.ecommerce.repository.TiendaRepository;
import com.upb.ecommerce.repository.UsuarioRepository;
import com.upb.ecommerce.repository.entities.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

@Service
public class CarritoService {

    private final CarritoRepository carritoRepository;
    private final DetalleCarritoRepository detalleCarritoRepository;
    private final ProductoRepository productoRepository;
    private final TiendaRepository tiendaRepository;
    private final UsuarioRepository usuarioRepository;

    public CarritoService(CarritoRepository carritoRepository,
                          DetalleCarritoRepository detalleCarritoRepository,
                          ProductoRepository productoRepository,
                          TiendaRepository tiendaRepository,
                          UsuarioRepository usuarioRepository) {
        this.carritoRepository = carritoRepository;
        this.detalleCarritoRepository = detalleCarritoRepository;
        this.productoRepository = productoRepository;
        this.tiendaRepository = tiendaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public CarritoResponse obtenerCarritoActivo(Long tiendaId, Long usuarioId) {
        Carrito carrito = carritoRepository
                .findByUsuarioIdAndTiendaIdAndEstado(usuarioId, tiendaId, "ACTIVO")
                .orElseThrow(() -> new RuntimeException("No hay carrito activo para este usuario"));
        return CarritoResponse.fromEntity(carrito);
    }

    @Transactional
    public CarritoResponse agregarItem(AgregarItemCarritoRequest request) {
        Tienda tienda = tiendaRepository.findById(request.getTiendaId())
                .orElseThrow(() -> new RuntimeException("Tienda no encontrada"));

        Usuario usuario = usuarioRepository.findById(request.getUsuarioId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Producto producto = productoRepository.findByIdAndTiendaId(request.getProductoId(), request.getTiendaId())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado en esta tienda"));

        if (producto.getStock() < request.getCantidad()) {
            throw new RuntimeException("Stock insuficiente. Disponible: " + producto.getStock());
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

        Optional<DetalleCarrito> detalleExistente = detalleCarritoRepository
                .findByCarritoIdAndProductoId(carrito.getId(), producto.getId());

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

        recalcularTotal(carrito);
        return CarritoResponse.fromEntity(carritoRepository.findById(carrito.getId()).orElseThrow());
    }

    @Transactional
    public CarritoResponse eliminarItem(Long carritoId, Long detalleId) {
        DetalleCarrito detalle = detalleCarritoRepository.findById(detalleId)
                .orElseThrow(() -> new RuntimeException("Item no encontrado en el carrito"));

        Carrito carrito = detalle.getCarrito();
        detalleCarritoRepository.delete(detalle);

        recalcularTotal(carrito);
        return CarritoResponse.fromEntity(carritoRepository.findById(carrito.getId()).orElseThrow());
    }

    private void recalcularTotal(Carrito carrito) {
        Carrito fresh = carritoRepository.findById(carrito.getId()).orElseThrow();
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
