package com.upb.ecommerce.core.service;

import com.upb.ecommerce.core.dto.request.LoginRequest;
import com.upb.ecommerce.core.dto.request.UsuarioRequest;
import com.upb.ecommerce.core.dto.response.UsuarioResponse;
import com.upb.ecommerce.data.repository.TiendaRepository;
import com.upb.ecommerce.data.repository.UsuarioRepository;
import com.upb.ecommerce.domain.entities.Tienda;
import com.upb.ecommerce.domain.entities.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final TiendaRepository tiendaRepository;

    public UsuarioService(UsuarioRepository usuarioRepository, TiendaRepository tiendaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.tiendaRepository = tiendaRepository;
    }

    public List<UsuarioResponse> listarPorTienda(Long tiendaId) {
        return usuarioRepository.findAll().stream()
                .filter(u -> u.getTienda().getId().equals(tiendaId))
                .map(UsuarioResponse::fromEntity)
                .toList();
    }

    public UsuarioResponse obtenerPorId(Long id) {
        return UsuarioResponse.fromEntity(
                usuarioRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Usuario no encontrado")));
    }

    @Transactional
    public UsuarioResponse registrar(UsuarioRequest request) {
        Tienda tienda = tiendaRepository.findById(request.getTiendaId())
                .orElseThrow(() -> new RuntimeException("Tienda no encontrada"));

        if (usuarioRepository.findByEmailAndTiendaId(request.getEmail(), tienda.getId()).isPresent()) {
            throw new RuntimeException("Ya existe un usuario con ese email en esta tienda");
        }
        if (!request.getRol().equals("ADMIN") && !request.getRol().equals("CLIENTE")) {
            throw new RuntimeException("Rol inválido. Debe ser ADMIN o CLIENTE");
        }

        Usuario usuario = new Usuario();
        usuario.setTienda(tienda);
        usuario.setNombre(request.getNombre());
        usuario.setEmail(request.getEmail());
        usuario.setPassword(request.getPassword()); // TODO: BCrypt
        usuario.setRol(request.getRol());
        return UsuarioResponse.fromEntity(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponse actualizar(Long id, UsuarioRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        usuario.setNombre(request.getNombre());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            usuario.setPassword(request.getPassword()); // TODO: BCrypt
        }
        return UsuarioResponse.fromEntity(usuarioRepository.save(usuario));
    }

    public UsuarioResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository
                .findByEmailAndTiendaId(request.getEmail(), request.getTiendaId())
                .orElseThrow(() -> new RuntimeException("Credenciales inválidas"));

        if (!usuario.getEstado()) {
            throw new RuntimeException("Usuario inactivo");
        }
        if (!usuario.getPassword().equals(request.getPassword())) { // TODO: BCrypt
            throw new RuntimeException("Credenciales inválidas");
        }
        return UsuarioResponse.fromEntity(usuario);
    }

    @Transactional
    public void desactivar(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        usuario.setEstado(false);
        usuarioRepository.save(usuario);
    }
}
