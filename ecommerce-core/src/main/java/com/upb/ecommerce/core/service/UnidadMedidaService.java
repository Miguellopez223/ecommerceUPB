package com.upb.ecommerce.core.service;

import com.upb.ecommerce.core.dto.request.UnidadMedidaRequest;
import com.upb.ecommerce.core.dto.response.UnidadMedidaResponse;
import com.upb.ecommerce.core.exception.NotDataFoundException;
import com.upb.ecommerce.data.repository.TiendaRepository;
import com.upb.ecommerce.data.repository.UnidadMedidaRepository;
import com.upb.ecommerce.domain.entities.Tienda;
import com.upb.ecommerce.domain.entities.UnidadMedida;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UnidadMedidaService {

    private final UnidadMedidaRepository unidadRepository;
    private final TiendaRepository tiendaRepository;

    public UnidadMedidaService(UnidadMedidaRepository unidadRepository,
                               TiendaRepository tiendaRepository) {
        this.unidadRepository = unidadRepository;
        this.tiendaRepository = tiendaRepository;
    }

    public List<UnidadMedidaResponse> listarPorTienda(Long tiendaId) {
        return unidadRepository.findByTiendaIdAndEstadoTrue(tiendaId)
                .stream().map(UnidadMedidaResponse::fromEntity).toList();
    }

    @Transactional
    public UnidadMedidaResponse crear(UnidadMedidaRequest request) {
        Tienda tienda = tiendaRepository.findById(request.getTiendaId())
                .orElseThrow(() -> new NotDataFoundException("Tienda no encontrada"));

        UnidadMedida unidad = new UnidadMedida();
        unidad.setTienda(tienda);
        unidad.setNombre(request.getNombre());
        unidad.setAbreviatura(request.getAbreviatura());
        return UnidadMedidaResponse.fromEntity(unidadRepository.save(unidad));
    }

    @Transactional
    public UnidadMedidaResponse actualizar(Long id, UnidadMedidaRequest request) {
        UnidadMedida unidad = unidadRepository.findByIdAndTiendaId(id, request.getTiendaId())
                .orElseThrow(() -> new NotDataFoundException("Unidad de medida no encontrada"));
        unidad.setNombre(request.getNombre());
        unidad.setAbreviatura(request.getAbreviatura());
        return UnidadMedidaResponse.fromEntity(unidadRepository.save(unidad));
    }

    @Transactional
    public void eliminar(Long tiendaId, Long id) {
        UnidadMedida unidad = unidadRepository.findByIdAndTiendaId(id, tiendaId)
                .orElseThrow(() -> new NotDataFoundException("Unidad de medida no encontrada"));
        unidad.setEstado(false);
        unidadRepository.save(unidad);
    }
}
