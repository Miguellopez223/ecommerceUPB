package com.upb.ecommerce.api.config;

import com.upb.ecommerce.api.exception.InvalidJwtAuthenticationException;
import com.upb.ecommerce.core.dto.response.LoginResponse;
import com.upb.ecommerce.core.service.UsuarioService;
import com.upb.ecommerce.domain.entities.Usuario;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.io.Serializable;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;

@Slf4j
@Component
public class JwtTokenProvider implements Serializable {

    @Value("${security.jwt.token.secret-key}")
    private String secretKey;
    private byte[] secretKeyBytes;

    @Value("${security.jwt.token.expire-length:480}")
    private int validityInMinutes;

    @Autowired
    private UsuarioService usuarioService;

    @PostConstruct
    protected void init() {
        secretKeyBytes = Base64.getDecoder().decode(secretKey);
    }

    public LoginResponse createToken(Usuario usuario) {
        Date now = new Date();
        Date expiration = plusMinutes(now, validityInMinutes);

        Claims claims = Jwts.claims()
                .subject(usuario.getUsername())       // email
                .id(usuario.getId().toString())       // id para buscar en validación
                .issuedAt(now)
                .expiration(expiration)
                .build();

        SecretKey key = Keys.hmacShaKeyFor(secretKeyBytes);
        String token = Jwts.builder()
                .claims(claims)
                .signWith(key)
                .compact();

        return LoginResponse.builder()
                .accessToken(token)
                .idToken(token)          // mismo token para id (no usamos OpenID separado)
                .refreshToken(token)     // refresh aún no implementado — placeholder
                .tokenType("bearer")
                .expiresIn((long) validityInMinutes * 60 * 1000)
                .expiresAt(expiration.getTime())
                .build();
    }

    public String resolveToken(String bearerToken) {
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    public Optional<Authentication> validateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(secretKeyBytes);
            Jws<Claims> claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);

            if (claims.getPayload().getExpiration().after(new Date())) {
                Long userId = Long.parseLong(claims.getPayload().getId());
                // Usuario ya implementa UserDetails — se usa directamente como principal
                Usuario usuario = usuarioService.findByIdForSession(userId)
                        .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado en sesión"));
                return Optional.of(
                        new UsernamePasswordAuthenticationToken(usuario, "", usuario.getAuthorities())
                );
            }
            return Optional.empty();

        } catch (ExpiredJwtException e) {
            log.warn("La sesión del usuario ha expirado");
            throw e;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Error al validar el TOKEN", e);
            throw new InvalidJwtAuthenticationException("Token JWT caducado o no válido.");
        }
    }

    private Date plusMinutes(Date date, int minutes) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.MINUTE, minutes);
        return cal.getTime();
    }
}
