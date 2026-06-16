package com.upb.ecommerce.api.config;

import com.upb.ecommerce.api.exception.InvalidJwtAuthenticationException;
import com.upb.ecommerce.core.exception.NotDataFoundException;
import com.upb.ecommerce.core.exception.OperationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

/**
 * Manejo centralizado de errores. Cada excepción se traduce a un {@link ProblemDetail}
 * (formato estándar RFC 7807 "Problem Details for HTTP APIs"), un JSON uniforme con
 * {@code type}, {@code title}, {@code status} y {@code detail} (el mensaje para el cliente):
 *
 * <ul>
 *   <li>{@link NotDataFoundException} → 404 (el recurso no existe).</li>
 *   <li>{@link OperationException} y validación de DTOs → 400 (datos inválidos, culpa del cliente).</li>
 *   <li>{@link InvalidJwtAuthenticationException} → 401 (token inválido).</li>
 *   <li>{@link ResponseStatusException} → respeta el código que fijó el controller (401, 502, etc.).</li>
 *   <li>Cualquier otra excepción → 500 (error inesperado del servidor, sin filtrar detalles internos).</li>
 * </ul>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotDataFoundException.class)
    public ProblemDetail handleNotFound(NotDataFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Recurso no encontrado", ex.getMessage());
    }

    @ExceptionHandler(OperationException.class)
    public ProblemDetail handleOperation(OperationException ex) {
        log.warn("Operación inválida: {}", ex.getMessage());
        return problem(HttpStatus.BAD_REQUEST, "Solicitud inválida", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> errores.put(e.getField(), e.getDefaultMessage()));
        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "Validación fallida",
                "Hay campos inválidos en la solicitud.");
        // Detalle por campo, como propiedad extra del Problem Details.
        pd.setProperty("errors", errores);
        return pd;
    }

    @ExceptionHandler(InvalidJwtAuthenticationException.class)
    public ProblemDetail handleInvalidJwt(InvalidJwtAuthenticationException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "No autorizado", ex.getMessage());
    }

    /**
     * Excepciones donde el controller ya decidió el código HTTP (p. ej. AuthController lanza
     * 401 para credenciales malas o 502 si falla el sistema externo). Respetamos ese código
     * en vez de convertirlo en 500.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        return problem(status, status.getReasonPhrase(), ex.getReason());
    }

    /**
     * Red de seguridad: cualquier error no contemplado es culpa del servidor (500).
     * Registramos la causa real en el log pero no la exponemos al cliente.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        log.error("Error inesperado del servidor", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno",
                "Ocurrió un error interno. Inténtalo de nuevo más tarde.");
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail != null ? detail : title);
        pd.setTitle(title);
        return pd;
    }
}
