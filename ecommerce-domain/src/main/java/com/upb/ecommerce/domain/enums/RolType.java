package com.upb.ecommerce.domain.enums;

/**
 * Roles disponibles para un usuario.
 * Se guardan como String en BD vía {@code @Enumerated(EnumType.STRING)},
 * preservando los valores existentes ("ADMIN", "CLIENTE").
 */
public enum RolType {
    ADMIN,
    CLIENTE
}
