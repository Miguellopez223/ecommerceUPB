package com.upb.ecommerce.core.service;

import com.upb.ecommerce.core.dto.request.MovimientoInventarioRequest;
import com.upb.ecommerce.core.dto.response.MovimientoInventarioResponse;
import com.upb.ecommerce.data.repository.MovimientoInventarioRepository;
import com.upb.ecommerce.data.repository.ProductoRepository;
import com.upb.ecommerce.data.repository.TiendaRepository;
import com.upb.ecommerce.data.repository.UsuarioRepository;
import com.upb.ecommerce.domain.entities.MovimientoInventario;
import com.upb.ecommerce.domain.entities.Producto;
import com.upb.ecommerce.domain.entities.Tienda;
import com.upb.ecommerce.domain.entities.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MovimientoInventarioService {

    private final MovimientoInventarioRepository movimientoRepository;
    private final ProductoRepository productoRepository;
    private final TiendaRepository tiendaRepository;
    private final UsuarioRepository usuarioRepository;

    public MovimientoInventarioService(MovimientoInventarioRepository movimientoRepository,
                                        ProductoRepository productoRepository,
                                        TiendaRepository tiendaRepository,
                                        UsuarioRepository usuarioRepository) {
        this.movimientoRepository = movimientoRepository;
        this.productoRepository = productoRepository;
        this.tiendaRepository = tiendaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public List<MovimientoInventarioResponse> listarPorProducto(Long productoId) {
        return movimientoRepository.findByProductoIdOrderByFechaDesc(productoId)
                .stream().map(MovimientoInventarioResponse::fromEntity).toList();
    }

    public List<MovimientoInventarioResponse> listarPorTienda(Long tiendaId) {
        return movimientoRepository.findByTiendaIdOrderByFechaDesc(tiendaId)
                .stream().map(MovimientoInventarioResponse::fromEntity).toList();
    }

    @Transactional
    public MovimientoInventarioResponse registrar(MovimientoInventarioRequest request) {
        Tienda tienda = tiendaRepository.findById(request.getTiendaId())
                .orElseThrow(() -> new RuntimeException("Tienda no encontrada"));
        Producto producto = productoRepository.findByIdAndTiendaId(request.getProductoId(), request.getTiendaId())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado en esta tienda"));

        if (!request.getTipo().equals("ENTRADA") && !request.getTipo().equals("SALIDA")) {
            throw new RuntimeException("Tipo inválido. Debe ser ENTRADA o SALIDA");
        }
        if (request.getTipo().equals("SALIDA") && producto.getStock() < request.getCantidad()) {
            throw new RuntimeException("Stock insuficiente. Disponible: " + producto.getStock());
        }

        if (request.getTipo().equals("ENTRADA")) {
            producto.setStock(producto.getStock() + request.getCantidad());
        } else {
            producto.setStock(producto.getStock() - request.getCantidad());
        }
        productoRepository.save(producto);

        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setTienda(tienda);
        movimiento.setProducto(producto);
        movimiento.setTipo(request.getTipo());
        movimiento.setCantidad(request.getCantidad());
        movimiento.setReferencia(request.getReferencia());

        if (request.getUsuarioId() != null) {
            Usuario usuario = usuarioRepository.findById(request.getUsuarioId())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            movimiento.setUsuario(usuario);
        }
        return MovimientoInventarioResponse.fromEntity(movimientoRepository.save(movimiento));
    }
}
