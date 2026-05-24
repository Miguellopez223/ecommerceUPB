package com.upb.ecommerce.domain.enums;

/**
 * Estados posibles de un usuario. Disponible para uso futuro.
 *
 * <p>NOTA: Actualmente {@code Usuario.estado} es {@code Boolean} para mantener
 * compatibilidad con el schema existente. Migrar a este enum requeriría:
 * <ul>
 *   <li>ALTER de la columna {@code estado} de BOOLEAN a VARCHAR(20)</li>
 *   <li>Cambiar el campo en {@code Usuario} a {@code UsuarioStatus} con {@code @Enumerated(EnumType.STRING)}</li>
 *   <li>Actualizar {@code isEnabled()} para retornar {@code status == ACTIVO}</li>
 * </ul>
 */
public enum UsuarioStatus {
    ACTIVO,
    INACTIVO,
    BLOQUEADO
}
