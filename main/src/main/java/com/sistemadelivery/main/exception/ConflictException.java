package com.sistemadelivery.main.exception;

import org.springframework.http.HttpStatus;

/**
 * 409: conflicto con el estado actual del recurso.
 * Se usa para stock insuficiente, transiciones inválidas,
 * correo duplicado y asignaciones concurrentes.
 */
public class ConflictException extends ApiException {

    public ConflictException(String codigo, String mensaje) {
        super(HttpStatus.CONFLICT, codigo, mensaje);
    }

    public static ConflictException de(String mensaje) {
        return new ConflictException("CONFLICTO", mensaje);
    }
}
