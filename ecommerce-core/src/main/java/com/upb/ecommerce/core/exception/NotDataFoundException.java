package com.upb.ecommerce.core.exception;

import lombok.Getter;

/**
 * Excepción lanzada cuando una búsqueda no encuentra el dato esperado.
 * Sustituye al {@code RuntimeException} genérico con mensaje "X no encontrado".
 */
@Getter
public class NotDataFoundException extends RuntimeException {

    public NotDataFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotDataFoundException(String message) {
        super(message);
    }
}
