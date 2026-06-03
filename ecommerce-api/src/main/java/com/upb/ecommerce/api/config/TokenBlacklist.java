package com.upb.ecommerce.api.config;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lista negra (denylist) de tokens JWT invalidados por logout.
 *
 * <p>El esquema de autenticación es stateless, por lo que un JWT sigue siendo
 * criptográficamente válido hasta su expiración. Para soportar "cerrar sesión"
 * de forma inmediata, los tokens cuya sesión se cierra se guardan aquí hasta
 * que expiran naturalmente; {@link JwtTokenFilter} rechaza cualquier petición
 * que use un token presente en esta lista.
 *
 * <p>Implementación en memoria (suficiente para una sola instancia). Las entradas
 * se purgan de forma perezosa al insertar y al consultar para no acumular tokens
 * ya expirados.
 */
@Component
public class TokenBlacklist {

    /** token -> instante de expiración (epoch millis). */
    private final Map<String, Long> blacklist = new ConcurrentHashMap<>();

    /**
     * Invalida un token hasta el instante en que habría expirado por sí mismo.
     *
     * @param token           el JWT en crudo (sin el prefijo "Bearer ")
     * @param expiresAtMillis epoch millis en que el token expira naturalmente
     */
    public void add(String token, long expiresAtMillis) {
        purgeExpired();
        blacklist.put(token, expiresAtMillis);
    }

    /** @return {@code true} si el token fue invalidado y aún no ha expirado. */
    public boolean contains(String token) {
        Long expiresAt = blacklist.get(token);
        if (expiresAt == null) {
            return false;
        }
        if (expiresAt < System.currentTimeMillis()) {
            blacklist.remove(token);
            return false;
        }
        return true;
    }

    private void purgeExpired() {
        long now = System.currentTimeMillis();
        blacklist.entrySet().removeIf(entry -> entry.getValue() < now);
    }
}
