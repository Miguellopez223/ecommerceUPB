package com.upb.ecommerce.api.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.session.SessionManagementFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.Serializable;

@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig implements Serializable {

    @Bean
    @Order(1)
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           CorsFilter corsFilter,
                                           JwtTokenFilter jwtTokenFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .addFilterBefore(corsFilter, SessionManagementFilter.class)
                .authorizeHttpRequests(auth -> auth
                        // Swagger
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        // Frontend SPA estatico (servido desde classpath:/static)
                        .requestMatchers(HttpMethod.GET,
                                "/", "/index.html", "/app.js", "/styles.css", "/favicon.ico"
                        ).permitAll()
                        // Endpoints publicos - registro, login (auth) y gestion de tiendas
                        .requestMatchers(HttpMethod.POST, "/api/auth").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/externo").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/usuarios/registrar").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/tiendas").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/tiendas").permitAll()
                        // Catalogo publico de una tienda por slug (sin login)
                        .requestMatchers(HttpMethod.GET, "/api/catalogo/**").permitAll()
                        // Webhook de Stereum: público porque Stereum no envía JWT;
                        // se autentica con la firma HMAC validada en el controlador.
                        .requestMatchers(HttpMethod.POST, "/api/webhooks/stereum/outbound").permitAll()
                        .requestMatchers("/error").anonymous()
                        // Todo lo demas requiere JWT
                        .anyRequest().authenticated()
                )
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()));

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
