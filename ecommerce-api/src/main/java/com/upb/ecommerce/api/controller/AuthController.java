package com.upb.ecommerce.api.controller;

import com.upb.ecommerce.api.config.JwtTokenProvider;
import com.upb.ecommerce.core.dto.request.LoginRequest;
import com.upb.ecommerce.core.dto.response.LoginResponse;
import com.upb.ecommerce.core.service.UsuarioService;
import com.upb.ecommerce.domain.entities.Usuario;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.springframework.http.ResponseEntity.ok;

/**
 * Endpoint de autenticación. Recibe email + password + tiendaId y devuelve un JWT.
 *
 * <p>Sigue el patrón del proyecto eventop (AuthController + AuthenticationManager)
 * pero adaptado al modelo multi-tienda: primero valida que el usuario existe en
 * la tienda solicitada, luego usa AuthenticationManager para verificar la contraseña.
 */
@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final UsuarioService usuarioService;
    private final AuthenticationManager authenticationManager;

    @PostMapping
    public ResponseEntity<LoginResponse> token(@Valid @RequestBody LoginRequest data) {
        try {
            LoginResponse token = auth(data);
            return ok(token);
        } catch (BadCredentialsException e) {
            log.error("BadCredentialsException al autenticar: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email o contraseña incorrectos");
        } catch (Exception e) {
            log.error("Error al autenticar el usuario {}", data.getEmail(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al autenticar");
        }
    }

    private LoginResponse auth(LoginRequest data) {
        log.info("Iniciando autenticación para email={} tiendaId={}", data.getEmail(), data.getTiendaId());

        // 1. Validar que el usuario existe en esta tienda específica (multi-tenant)
        Usuario usuario;
        try {
            Optional<Usuario> userOpt = usuarioService.findByEmailAndTiendaId(data.getEmail(), data.getTiendaId());
            if (userOpt.isEmpty()) {
                throw new BadCredentialsException("Email o contraseña son incorrectos");
            }
            usuario = userOpt.get();
        } catch (BadCredentialsException e) {
            throw e;
        } catch (Exception e) {
            log.error("No se encontró el usuario {} en la tienda {}", data.getEmail(), data.getTiendaId());
            throw new BadCredentialsException("Email o contraseña son incorrectos");
        }

        // 2. Autenticar con Spring Security (verifica contraseña vía PasswordEncoder)
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(data.getEmail(), data.getPassword())
            );
            log.info("Autenticado correctamente: {}", data.getEmail());
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(usuario, "", usuario.getAuthorities())
            );
            // 3. Generar y retornar JWT
            return jwtTokenProvider.createToken(usuario);
        } catch (BadCredentialsException e) {
            log.error("Contraseña incorrecta para {}: {}", data.getEmail(), e.getMessage());
            throw new BadCredentialsException("Email o contraseña son incorrectos");
        } catch (AuthenticationException e) {
            log.error("AuthenticationException: {}", e.getMessage());
            throw new BadCredentialsException("Email o contraseña son incorrectos");
        } catch (Exception e) {
            log.error("Error de autenticación", e);
            throw e;
        }
    }
}
