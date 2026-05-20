package com.upb.ecommerce.service;

import com.upb.ecommerce.dto.ProductoRequest;
import com.upb.ecommerce.dto.ProductoResponse;
import com.upb.ecommerce.repository.CategoriaRepository;
import com.upb.ecommerce.repository.ProductoRepository;
import com.upb.ecommerce.repository.TiendaRepository;
import com.upb.ecommerce.repository.entities.Categoria;
import com.upb.ecommerce.repository.entities.Producto;
import com.upb.ecommerce.repository.entities.Tienda;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final TiendaRepository tiendaRepository;
    private final CategoriaRepository categoriaRepository;

    public ProductoService(ProductoRepository productoRepository,
                           TiendaRepository tiendaRepository,
                           CategoriaRepository categoriaRepository) {
        this.productoRepository = productoRepository;
        this.tiendaRepository = tiendaRepository;
        this.categoriaRepository = categoriaRepository;
    }

    public List<ProductoResponse> listarPorTienda(Long tiendaId) {
        return productoRepository.findByTiendaIdAndEstadoTrue(tiendaId)
                .stream()
                .map(ProductoResponse::fromEntity)
                .toList();
    }

    public List<ProductoResponse> listarPorCategoria(Long tiendaId, Long categoriaId) {
        return productoRepository.findByTiendaIdAndCategoriaIdAndEstadoTrue(tiendaId, categoriaId)
                .stream()
                .map(ProductoResponse::fromEntity)
                .toList();
    }

    public ProductoResponse obtenerPorId(Long tiendaId, Long productoId) {
        Producto producto = productoRepository.findByIdAndTiendaId(productoId, tiendaId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
        return ProductoResponse.fromEntity(producto);
    }

    @Transactional
    public ProductoResponse crear(ProductoRequest request) {
        Tienda tienda = tiendaRepository.findById(request.getTiendaId())
                .orElseThrow(() -> new RuntimeException("Tienda no encontrada"));

        Producto producto = new Producto();
        producto.setTienda(tienda);
        producto.setNombre(request.getNombre());
        producto.setSlugProducto(request.getSlugProducto());
        producto.setDescripcionLarga(request.getDescripcionLarga());
        producto.setPrecio(request.getPrecio());
        producto.setPrecioCosto(request.getPrecioCosto());
        producto.setStock(request.getStock());
        producto.setImagenUrl(request.getImagenUrl());

        if (request.getStockMinimo() != null) {
            producto.setStockMinimo(request.getStockMinimo());
        }

        if (request.getCategoriaId() != null) {
            Categoria categoria = categoriaRepository.findById(request.getCategoriaId())
                    .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));
            producto.setCategoria(categoria);
        }

        return ProductoResponse.fromEntity(productoRepository.save(producto));
    }

    @Transactional
    public ProductoResponse actualizar(Long id, ProductoRequest request) {
        Producto producto = productoRepository.findByIdAndTiendaId(id, request.getTiendaId())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        producto.setNombre(request.getNombre());
        producto.setSlugProducto(request.getSlugProducto());
        producto.setDescripcionLarga(request.getDescripcionLarga());
        producto.setPrecio(request.getPrecio());
        producto.setPrecioCosto(request.getPrecioCosto());
        producto.setStock(request.getStock());
        producto.setImagenUrl(request.getImagenUrl());

        if (request.getStockMinimo() != null) {
            producto.setStockMinimo(request.getStockMinimo());
        }

        if (request.getCategoriaId() != null) {
            Categoria categoria = categoriaRepository.findById(request.getCategoriaId())
                    .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));
            producto.setCategoria(categoria);
        } else {
            producto.setCategoria(null);
        }

        return ProductoResponse.fromEntity(productoRepository.save(producto));
    }

    @Transactional
    public void eliminar(Long tiendaId, Long id) {
        Producto producto = productoRepository.findByIdAndTiendaId(id, tiendaId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
        producto.setEstado(false);
        productoRepository.save(producto);
    }
}
