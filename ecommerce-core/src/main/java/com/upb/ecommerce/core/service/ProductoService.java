package com.upb.ecommerce.core.service;

import com.upb.ecommerce.core.dto.request.ProductoRequest;
import com.upb.ecommerce.core.dto.response.ProductoResponse;
import com.upb.ecommerce.core.exception.NotDataFoundException;
import com.upb.ecommerce.data.repository.CategoriaRepository;
import com.upb.ecommerce.data.repository.ProductoRepository;
import com.upb.ecommerce.data.repository.TiendaRepository;
import com.upb.ecommerce.data.repository.UnidadMedidaRepository;
import com.upb.ecommerce.domain.entities.Categoria;
import com.upb.ecommerce.domain.entities.Producto;
import com.upb.ecommerce.domain.entities.Tienda;
import com.upb.ecommerce.domain.entities.UnidadMedida;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final TiendaRepository tiendaRepository;
    private final CategoriaRepository categoriaRepository;
    private final UnidadMedidaRepository unidadMedidaRepository;

    public ProductoService(ProductoRepository productoRepository,
                           TiendaRepository tiendaRepository,
                           CategoriaRepository categoriaRepository,
                           UnidadMedidaRepository unidadMedidaRepository) {
        this.productoRepository = productoRepository;
        this.tiendaRepository = tiendaRepository;
        this.categoriaRepository = categoriaRepository;
        this.unidadMedidaRepository = unidadMedidaRepository;
    }

    public List<ProductoResponse> listarPorTienda(Long tiendaId) {
        return productoRepository.findByTiendaIdAndEstadoTrue(tiendaId)
                .stream().map(ProductoResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public Page<ProductoResponse> listarPorTiendaPaginado(Long tiendaId, Pageable pageable) {
        return productoRepository.findByTiendaIdAndEstadoTrue(tiendaId, pageable)
                .map(ProductoResponse::fromEntity);
    }

    public List<ProductoResponse> listarPorCategoria(Long tiendaId, Long categoriaId) {
        return productoRepository.findByTiendaIdAndCategoriaIdAndEstadoTrue(tiendaId, categoriaId)
                .stream().map(ProductoResponse::fromEntity).toList();
    }

    public ProductoResponse obtenerPorId(Long tiendaId, Long productoId) {
        return ProductoResponse.fromEntity(
                productoRepository.findByIdAndTiendaId(productoId, tiendaId)
                        .orElseThrow(() -> new NotDataFoundException("Producto no encontrado")));
    }

    @Transactional
    public ProductoResponse crear(ProductoRequest request) {
        Tienda tienda = tiendaRepository.findById(request.getTiendaId())
                .orElseThrow(() -> new NotDataFoundException("Tienda no encontrada"));

        Producto producto = new Producto();
        producto.setTienda(tienda);
        producto.setNombre(request.getNombre());
        producto.setSlugProducto(request.getSlugProducto());
        producto.setDescripcionLarga(request.getDescripcionLarga());
        producto.setPrecio(request.getPrecio());
        producto.setPrecioCosto(request.getPrecioCosto());
        producto.setStock(request.getStock());
        producto.setImagenUrl(request.getImagenUrl());
        if (request.getStockMinimo() != null) producto.setStockMinimo(request.getStockMinimo());

        if (request.getCategoriaId() != null) {
            Categoria cat = categoriaRepository.findById(request.getCategoriaId())
                    .orElseThrow(() -> new NotDataFoundException("Categoría no encontrada"));
            producto.setCategoria(cat);
        }
        producto.setUnidadMedida(resolverUnidad(request.getUnidadMedidaId()));
        return ProductoResponse.fromEntity(productoRepository.save(producto));
    }

    @Transactional
    public ProductoResponse actualizar(Long id, ProductoRequest request) {
        Producto producto = productoRepository.findByIdAndTiendaId(id, request.getTiendaId())
                .orElseThrow(() -> new NotDataFoundException("Producto no encontrado"));

        producto.setNombre(request.getNombre());
        producto.setSlugProducto(request.getSlugProducto());
        producto.setDescripcionLarga(request.getDescripcionLarga());
        producto.setPrecio(request.getPrecio());
        producto.setPrecioCosto(request.getPrecioCosto());
        producto.setStock(request.getStock());
        producto.setImagenUrl(request.getImagenUrl());
        if (request.getStockMinimo() != null) producto.setStockMinimo(request.getStockMinimo());

        if (request.getCategoriaId() != null) {
            Categoria cat = categoriaRepository.findById(request.getCategoriaId())
                    .orElseThrow(() -> new NotDataFoundException("Categoría no encontrada"));
            producto.setCategoria(cat);
        } else {
            producto.setCategoria(null);
        }
        producto.setUnidadMedida(resolverUnidad(request.getUnidadMedidaId()));
        return ProductoResponse.fromEntity(productoRepository.save(producto));
    }

    /** Resuelve la unidad de medida (opcional); null si no se envía. */
    private UnidadMedida resolverUnidad(Long unidadMedidaId) {
        if (unidadMedidaId == null) return null;
        return unidadMedidaRepository.findById(unidadMedidaId)
                .orElseThrow(() -> new NotDataFoundException("Unidad de medida no encontrada"));
    }

    @Transactional
    public void eliminar(Long tiendaId, Long id) {
        Producto producto = productoRepository.findByIdAndTiendaId(id, tiendaId)
                .orElseThrow(() -> new NotDataFoundException("Producto no encontrado"));
        producto.setEstado(false);
        productoRepository.save(producto);
    }
}
