package com.upb.ecommerce.core.exception;

import lombok.Getter;

/**
 * Excepción para errores de negocio causados por el cliente: datos inválidos o reglas
 * de negocio que no se cumplen (stock insuficiente, estado no válido, email duplicado, etc.).
 *
 * <p>El {@code GlobalExceptionHandler} la traduce a HTTP 400 (Bad Request) con el mensaje
 * para que el cliente entienda qué hizo mal. Se diferencia de {@link NotDataFoundException}
 * (404, el recurso no existe) y de los errores inesperados del servidor (500).
 */
@Getter
public class OperationException extends RuntimeException {

    public OperationException(String message) {
        super(message);
    }

    public OperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
