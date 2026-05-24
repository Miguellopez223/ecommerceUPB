package com.upb.ecommerce.core.service;

import com.upb.ecommerce.data.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Slf4j
@Service
public class UsuarioDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    /**
     * Carga un usuario por su email (username en Spring Security).
     * En un sistema multi-tienda el email puede repetirse entre tiendas;
     * este método retorna el primer usuario activo — se usa principalmente
     * como parte del contrato de Spring Security, mientras que el flujo JWT
     * valida directamente por ID.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.info("Cargando usuario por email: {}", email);
        return usuarioRepository.findFirstByEmailAndEstadoTrue(email)
                .orElseThrow(() -> new UsernameNotFoundException("No existe el usuario: " + email));
    }
}
