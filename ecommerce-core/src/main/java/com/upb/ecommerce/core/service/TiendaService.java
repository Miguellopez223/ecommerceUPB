package com.upb.ecommerce.core.service;

import com.upb.ecommerce.core.dto.request.TiendaRequest;
import com.upb.ecommerce.core.dto.response.TiendaResponse;
import com.upb.ecommerce.data.repository.TiendaRepository;
import com.upb.ecommerce.domain.entities.Tienda;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TiendaService {

    private final TiendaRepository tiendaRepository;

    public TiendaService(TiendaRepository tiendaRepository) {
        this.tiendaRepository = tiendaRepository;
    }

    public List<TiendaResponse> listarTodas() {
        return tiendaRepository.findAll().stream()
                .map(TiendaResponse::fromEntity)
                .toList();
    }

    public TiendaResponse obtenerPorId(Long id) {
        return TiendaResponse.fromEntity(
                tiendaRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Tienda no encontrada")));
    }

    public TiendaResponse obtenerPorSlug(String slug) {
        return TiendaResponse.fromEntity(
                tiendaRepository.findBySlug(slug)
                        .orElseThrow(() -> new RuntimeException("Tienda no encontrada")));
    }

    @Transactional
    public TiendaResponse crear(TiendaRequest request) {
        if (tiendaRepository.findBySlug(request.getSlug()).isPresent()) {
            throw new RuntimeException("Ya existe una tienda con el slug: " + request.getSlug());
        }
        Tienda tienda = new Tienda();
        tienda.setNombre(request.getNombre());
        tienda.setSlug(request.getSlug());
        tienda.setTelefonoContacto(request.getTelefonoContacto());
        tienda.setEmailContacto(request.getEmailContacto());
        tienda.setLogoUrl(request.getLogoUrl());
        return TiendaResponse.fromEntity(tiendaRepository.save(tienda));
    }

    @Transactional
    public TiendaResponse actualizar(Long id, TiendaRequest request) {
        Tienda tienda = tiendaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tienda no encontrada"));
        tienda.setNombre(request.getNombre());
        tienda.setTelefonoContacto(request.getTelefonoContacto());
        tienda.setEmailContacto(request.getEmailContacto());
        tienda.setLogoUrl(request.getLogoUrl());
        return TiendaResponse.fromEntity(tiendaRepository.save(tienda));
    }

    @Transactional
    public void desactivar(Long id) {
        Tienda tienda = tiendaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tienda no encontrada"));
        tienda.setEstado(false);
        tiendaRepository.save(tienda);
    }
}
