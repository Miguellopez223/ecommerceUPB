package com.upb.ecommerce.service;

import com.upb.ecommerce.dto.DireccionEnvioRequest;
import com.upb.ecommerce.dto.DireccionEnvioResponse;
import com.upb.ecommerce.repository.DireccionEnvioRepository;
import com.upb.ecommerce.repository.UsuarioRepository;
import com.upb.ecommerce.repository.entities.DireccionEnvio;
import com.upb.ecommerce.repository.entities.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DireccionEnvioService {

    private final DireccionEnvioRepository direccionEnvioRepository;
    private final UsuarioRepository usuarioRepository;

    public DireccionEnvioService(DireccionEnvioRepository direccionEnvioRepository,
                                  UsuarioRepository usuarioRepository) {
        this.direccionEnvioRepository = direccionEnvioRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public List<DireccionEnvioResponse> listarPorUsuario(Long usuarioId) {
        return direccionEnvioRepository.findByUsuarioIdAndEstadoTrue(usuarioId)
                .stream()
                .map(DireccionEnvioResponse::fromEntity)
                .toList();
    }

    public DireccionEnvioResponse obtenerPorId(Long id) {
        return DireccionEnvioResponse.fromEntity(
                direccionEnvioRepository.findById(id)
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
        return DireccionEnvioResponse.fromEntity(direccionEnvioRepository.save(direccion));
    }

    @Transactional
    public DireccionEnvioResponse actualizar(Long id, DireccionEnvioRequest request) {
        DireccionEnvio direccion = direccionEnvioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dirección no encontrada"));
        direccion.setDireccionCalle(request.getDireccionCalle());
        direccion.setCiudad(request.getCiudad());
        direccion.setReferencias(request.getReferencias());
        return DireccionEnvioResponse.fromEntity(direccionEnvioRepository.save(direccion));
    }

    @Transactional
    public void eliminar(Long id) {
        DireccionEnvio direccion = direccionEnvioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dirección no encontrada"));
        direccion.setEstado(false);
        direccionEnvioRepository.save(direccion);
    }
}
