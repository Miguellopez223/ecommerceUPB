package com.upb.ecommerce.core.service;

import com.upb.ecommerce.core.dto.request.DireccionEnvioRequest;
import com.upb.ecommerce.core.dto.response.DireccionEnvioResponse;
import com.upb.ecommerce.data.repository.DireccionEnvioRepository;
import com.upb.ecommerce.data.repository.UsuarioRepository;
import com.upb.ecommerce.domain.entities.DireccionEnvio;
import com.upb.ecommerce.domain.entities.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DireccionEnvioService {

    private final DireccionEnvioRepository direccionRepository;
    private final UsuarioRepository usuarioRepository;

    public DireccionEnvioService(DireccionEnvioRepository direccionRepository,
                                  UsuarioRepository usuarioRepository) {
        this.direccionRepository = direccionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public List<DireccionEnvioResponse> listarPorUsuario(Long usuarioId) {
        return direccionRepository.findByUsuarioIdAndEstadoTrue(usuarioId)
                .stream().map(DireccionEnvioResponse::fromEntity).toList();
    }

    public DireccionEnvioResponse obtenerPorId(Long id) {
        return DireccionEnvioResponse.fromEntity(
                direccionRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Dirección no encontrada")));
    }

    @Transactional
    public DireccionEnvioResponse crear(DireccionEnvioRequest request) {
        Usuario usuario = usuarioRepository.findById(request.getUsuarioId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        DireccionEnvio direccion = new DireccionEnvio();
        direccion.setUsuario(usuario);
        direccion.setDireccionCalle(request.getDireccionCalle());
        direccion.setCiudad(request.getCiudad());
        direccion.setReferencias(request.getReferencias());
        return DireccionEnvioResponse.fromEntity(direccionRepository.save(direccion));
    }

    @Transactional
    public DireccionEnvioResponse actualizar(Long id, DireccionEnvioRequest request) {
        DireccionEnvio direccion = direccionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dirección no encontrada"));
        direccion.setDireccionCalle(request.getDireccionCalle());
        direccion.setCiudad(request.getCiudad());
        direccion.setReferencias(request.getReferencias());
        return DireccionEnvioResponse.fromEntity(direccionRepository.save(direccion));
    }

    @Transactional
    public void eliminar(Long id) {
        DireccionEnvio direccion = direccionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dirección no encontrada"));
        direccion.setEstado(false);
        direccionRepository.save(direccion);
    }
}
