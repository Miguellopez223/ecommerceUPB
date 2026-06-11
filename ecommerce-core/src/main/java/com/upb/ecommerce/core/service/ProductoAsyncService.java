package com.upb.ecommerce.core.service;

import com.upb.ecommerce.core.exception.NotDataFoundException;
import com.upb.ecommerce.data.repository.MovimientoInventarioRepository;
import com.upb.ecommerce.data.repository.ProductoRepository;
import com.upb.ecommerce.domain.entities.MovimientoInventario;
import com.upb.ecommerce.domain.entities.Producto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// --- PREGUNTA 5 ---
@Slf4j
@Service
public class ProductoAsyncService {

    private final ProductoRepository productoRepository;
    private final MovimientoInventarioRepository movimientoRepository;

    public ProductoAsyncService(ProductoRepository productoRepository,
                                MovimientoInventarioRepository movimientoRepository) {
        this.productoRepository = productoRepository;
        this.movimientoRepository = movimientoRepository;
    }

    // --- PREGUNTA 5 ---
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void actualizarStockAsync(Long productoId, int nuevoStock) {
        log.info("[async] Hilo '{}' — iniciando ajuste de stock del producto {}",
                Thread.currentThread().getName(), productoId);

        // --- PREGUNTA 5 ---
        // Buscar el REGISTRO A MODIFICAR. Si NO se encuentra, lanzar NotDataFoundException
        // lo que marca la NUEVA transacción para ROLLBACK: no se
        // persiste ningún cambio.
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new NotDataFoundException(
                        "Producto no encontrado id=" + productoId + " — se realiza ROLLBACK"));

        // --- PREGUNTA 5 ---
        int stockAnterior = producto.getStock();
        producto.setStock(nuevoStock);                 // (1) actualización de la entidad
        productoRepository.save(producto);

        // --- PREGUNTA 5 ---
        // (2) auditoría del ajuste. Solo se registra si el stock realmente cambió (delta != 0):
        int delta = nuevoStock - stockAnterior;
        if (delta != 0) {
            MovimientoInventario movimiento = new MovimientoInventario();
            movimiento.setTienda(producto.getTienda());
            movimiento.setProducto(producto);
            movimiento.setTipo(delta > 0 ? "ENTRADA" : "SALIDA");
            movimiento.setCantidad(Math.abs(delta));
            movimiento.setReferencia("Ajuste asíncrono de stock (PREGUNTA 5)");
            movimientoRepository.save(movimiento);
        }

        log.info("[async] Stock del producto {} actualizado {} -> {} (commit de la nueva transacción)",
                productoId, stockAnterior, nuevoStock);
    }
}
