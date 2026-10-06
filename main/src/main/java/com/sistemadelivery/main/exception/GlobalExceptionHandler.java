package com.sistemadelivery.main.exception;

import com.sistemadelivery.main.dto.response.DetalleError;
import com.sistemadelivery.main.dto.response.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

/**
 * Manejador global de excepciones: garantiza un formato de error consistente
 * y evita exponer trazas o información sensible al cliente.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    /** Excepciones de dominio: conservan su código HTTP y código de error. */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> manejarApi(ApiException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(ErrorResponse.simple(ex.getCodigo(), ex.getMessage()));
    }

    /** Validación de los DTO de entrada (@Valid). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> manejarValidacion(MethodArgumentNotValidException ex) {
        List<DetalleError> detalles = ex.getBindingResult().getFieldErrors().stream()
                .map(this::aDetalle)
                .toList();
        List<DetalleError> globales = ex.getBindingResult().getGlobalErrors().stream()
                .map(e -> new DetalleError("(cuerpo)", e.getDefaultMessage()))
                .toList();
        List<DetalleError> todos = new java.util.ArrayList<>(detalles);
        todos.addAll(globales);
        return ResponseEntity.badRequest()
                .body(ErrorResponse.conDetalles("VALIDACION_FALLIDA",
                        "La solicitud contiene datos inválidos", todos));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> manejarRestriccion(ConstraintViolationException ex) {
        List<DetalleError> detalles = ex.getConstraintViolations().stream()
                .map(v -> new DetalleError(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        return ResponseEntity.badRequest()
                .body(ErrorResponse.conDetalles("VALIDACION_FALLIDA",
                        "La solicitud contiene datos inválidos", detalles));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> manejarCuerpo(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.simple("CUERPO_INVALIDO", "El cuerpo de la solicitud no es válido"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> manejarTipoParametro(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.simple("PARAMETRO_INVALIDO",
                        "El parámetro '" + ex.getName() + "' tiene un valor inválido"));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> manejarParametroFaltante(MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.simple("PARAMETRO_FALTANTE",
                        "Falta el parámetro obligatorio '" + ex.getParameterName() + "'"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> manejarArgumentoIllegal(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.simple("ARGUMENTO_INVALIDO", ex.getMessage()));
    }

    /** Denegación de acceso emitida por Spring Security (403). */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> manejarAccesoDenegado(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.simple("ACCESO_DENEGADO", "No tiene permisos para realizar esta operación"));
    }

    /** Fallas de autenticación no capturadas por el filtro (401). */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> manejarAutenticacion(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.simple("NO_AUTENTICADO", "Credenciales o token inválidos"));
    }

    /** Carreras detectadas por JPA (@Version) o bloqueos no obtenidos a tiempo. */
    @ExceptionHandler({OptimisticLockingFailureException.class, PessimisticLockingFailureException.class})
    public ResponseEntity<ErrorResponse> manejarConcurrencia(RuntimeException ex) {
        log.warn("Conflicto de concurrencia: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.simple("CONFLICTO_CONCURRENCIA",
                        "La operación entró en conflicto con otra solicitud, inténtelo de nuevo"));
    }

    /** Violaciones de restricciones UNIQUE / CHECK / FK de la base de datos. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> manejarIntegridad(DataIntegrityViolationException ex) {
        log.warn("Violación de integridad: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.simple("INTEGRIDAD_DATOS",
                        "La operación viola una restricción de integridad de datos"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> manejarMetodo(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ErrorResponse.simple("METODO_NO_PERMITIDO", "Método HTTP no soportado para este recurso"));
    }

    /** Último recurso: error 500 sin exponer trazas internas. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> manejarExcepcionGeneral(Exception ex) {
        log.error("Error inesperado: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(ErrorResponse.simple("ERROR_INTERNO",
                        "Ocurrió un error inesperado, contacte al administrador"));
    }

    private DetalleError aDetalle(FieldError error) {
        return new DetalleError(error.getField(), error.getDefaultMessage());
    }
}
