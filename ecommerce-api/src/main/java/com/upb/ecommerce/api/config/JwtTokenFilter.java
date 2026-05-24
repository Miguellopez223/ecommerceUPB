package com.upb.ecommerce.api.config;

import com.upb.ecommerce.api.exception.InvalidJwtAuthenticationException;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.Serializable;
import java.util.Optional;

@Slf4j
@Component
@AllArgsConstructor
public class JwtTokenFilter extends OncePerRequestFilter implements Serializable {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String token = jwtTokenProvider.resolveToken(request.getHeader("Authorization"));
            if (token == null) {
                filterChain.doFilter(request, response);
                return;
            }

            try {
                Optional<Authentication> optionalAuth = jwtTokenProvider.validateToken(token);
                if (optionalAuth.isPresent()) {
                    SecurityContextHolder.getContext().setAuthentication(optionalAuth.get());
                    filterChain.doFilter(request, response);
                    return;
                }
                log.error("No se logró validar el JWT — devolviendo 401");
                writeUnauthorized(response);

            } catch (UsernameNotFoundException | ExpiredJwtException | InvalidJwtAuthenticationException e) {
                log.error("Error validando JWT: {}", e.getMessage());
                writeUnauthorized(response);
            }

        } catch (Exception e) {
            log.error("Excepción genérica al validar el JWT", e);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(new ObjectMapper().writeValueAsString(HttpStatus.INTERNAL_SERVER_ERROR));
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.getWriter().write("{\"error\":\"No autorizado\"}");
    }
}
