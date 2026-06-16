package com.upb.ecommerce.core.service;

import com.upb.ecommerce.core.dto.request.LoginRequest;
import com.upb.ecommerce.core.dto.request.UsuarioRequest;
import com.upb.ecommerce.core.dto.response.UsuarioResponse;
import com.upb.ecommerce.core.exception.NotDataFoundException;
import com.upb.ecommerce.core.exception.OperationException;
import com.upb.ecommerce.data.repository.TiendaRepository;
import com.upb.ecommerce.data.repository.UsuarioRepository;
import com.upb.ecommerce.domain.entities.Tienda;
import com.upb.ecommerce.domain.entities.Usuario;
import com.upb.ecommerce.domain.enums.RolType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final TiendaRepository tiendaRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          TiendaRepository tiendaRepository,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.tiendaRepository = tiendaRepository;
        this.passwordEncoder = passwordEncoder;
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
                        .orElseThrow(() -> new NotDataFoundException("Usuario no encontrado")));
    }

    @Transactional
    public UsuarioResponse registrar(UsuarioRequest request) {
        Tienda tienda = tiendaRepository.findById(request.getTiendaId())
                .orElseThrow(() -> new NotDataFoundException("Tienda no encontrada"));

        if (usuarioRepository.findByEmailAndTiendaId(request.getEmail(), tienda.getId()).isPresent()) {
            throw new OperationException("Ya existe un usuario con ese email en esta tienda");
        }

        Usuario usuario = new Usuario();
        usuario.setTienda(tienda);
        usuario.setNombre(request.getNombre());
        usuario.setEmail(request.getEmail());
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario.setNumeroWhatsapp(request.getNumeroWhatsapp());
        // Seguridad: el auto-registro es público, por lo que SIEMPRE se crea como
        // CLIENTE (no visible en catálogo) para evitar escalada de privilegios desde el
        // endpoint abierto.
        usuario.setRol(RolType.CLIENTE);
        usuario.setVisibleCatalogo(false);
        return UsuarioResponse.fromEntity(usuarioRepository.save(usuario));
    }

    /**
     * Alta de usuario por un ADMIN: respeta el rol enviado y permite configurar el número
     * de WhatsApp y la visibilidad en el catálogo público.
     */
    @Transactional
    public UsuarioResponse crearPorAdmin(UsuarioRequest request) {
        Tienda tienda = tiendaRepository.findById(request.getTiendaId())
                .orElseThrow(() -> new NotDataFoundException("Tienda no encontrada"));

        if (usuarioRepository.findByEmailAndTiendaId(request.getEmail(), tienda.getId()).isPresent()) {
            throw new OperationException("Ya existe un usuario con ese email en esta tienda");
        }

        Usuario usuario = new Usuario();
        usuario.setTienda(tienda);
        usuario.setNombre(request.getNombre());
        usuario.setEmail(request.getEmail());
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario.setRol(request.getRol());
        usuario.setNumeroWhatsapp(request.getNumeroWhatsapp());
        usuario.setVisibleCatalogo(Boolean.TRUE.equals(request.getVisibleCatalogo()));
        return UsuarioResponse.fromEntity(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponse actualizar(Long id, UsuarioRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new NotDataFoundException("Usuario no encontrado"));
        usuario.setNombre(request.getNombre());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        usuario.setNumeroWhatsapp(request.getNumeroWhatsapp());
        if (request.getVisibleCatalogo() != null) {
            usuario.setVisibleCatalogo(request.getVisibleCatalogo());
        }
        return UsuarioResponse.fromEntity(usuarioRepository.save(usuario));
    }

    /**
     * Busca un usuario por email + tiendaId. Usado por AuthController para validar
     * que el usuario existe en la tienda solicitada antes de autenticar.
     */
    public Optional<Usuario> findByEmailAndTiendaId(String email, Long tiendaId) {
        return usuarioRepository.findByEmailAndTiendaId(email, tiendaId);
    }

    /**
     * Valida credenciales completas (email + tiendaId + password) y devuelve el Usuario.
     * Conservado para usos que no requieran AuthenticationManager.
     */
    public Usuario validarCredenciales(LoginRequest request) {
        Usuario usuario = usuarioRepository
                .findByEmailAndTiendaId(request.getEmail(), request.getTiendaId())
                .orElseThrow(() -> new OperationException("Credenciales inválidas"));

        if (!usuario.getEstado()) {
            throw new OperationException("Usuario inactivo");
        }
        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            throw new OperationException("Credenciales inválidas");
        }
        return usuario;
    }

    /**
     * Busca un usuario activo por ID para validar la sesión JWT en cada request.
     * Usa la query JPQL optimizada que solo carga (id, email, rol).
     */
    public Optional<Usuario> findByIdForSession(Long id) {
        return usuarioRepository.findByIdToValidateSession(id);
    }

    @Transactional
    public void desactivar(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new NotDataFoundException("Usuario no encontrado"));
        usuario.setEstado(false);
        usuarioRepository.save(usuario);
    }

    @Transactional
    public UsuarioResponse reactivar(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new NotDataFoundException("Usuario no encontrado"));
        usuario.setEstado(true);
        return UsuarioResponse.fromEntity(usuarioRepository.save(usuario));
    }
}
