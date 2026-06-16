package com.upb.ecommerce.core.service;

import com.upb.ecommerce.core.dto.request.CategoriaRequest;
import com.upb.ecommerce.core.dto.response.CategoriaResponse;
import com.upb.ecommerce.core.exception.NotDataFoundException;
import com.upb.ecommerce.data.repository.CategoriaRepository;
import com.upb.ecommerce.data.repository.TiendaRepository;
import com.upb.ecommerce.domain.entities.Categoria;
import com.upb.ecommerce.domain.entities.Tienda;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final TiendaRepository tiendaRepository;

    public CategoriaService(CategoriaRepository categoriaRepository, TiendaRepository tiendaRepository) {
        this.categoriaRepository = categoriaRepository;
        this.tiendaRepository = tiendaRepository;
    }

    public List<CategoriaResponse> listarPorTienda(Long tiendaId) {
        return categoriaRepository.findByTiendaIdAndEstadoTrue(tiendaId)
                .stream().map(CategoriaResponse::fromEntity).toList();
    }

    @Transactional
    public CategoriaResponse crear(CategoriaRequest request) {
        Tienda tienda = tiendaRepository.findById(request.getTiendaId())
                .orElseThrow(() -> new NotDataFoundException("Tienda no encontrada"));
        Categoria categoria = new Categoria();
        categoria.setTienda(tienda);
        categoria.setNombre(request.getNombre());
        return CategoriaResponse.fromEntity(categoriaRepository.save(categoria));
    }

    @Transactional
    public CategoriaResponse actualizar(Long id, CategoriaRequest request) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new NotDataFoundException("Categoría no encontrada"));
        categoria.setNombre(request.getNombre());
        return CategoriaResponse.fromEntity(categoriaRepository.save(categoria));
    }

    @Transactional
    public void eliminar(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new NotDataFoundException("Categoría no encontrada"));
        categoria.setEstado(false);
        categoriaRepository.save(categoria);
    }
}
