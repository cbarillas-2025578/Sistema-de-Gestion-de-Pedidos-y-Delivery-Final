package com.sistemadelivery.main.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base de todas las excepciones de la API.
 * Cada subclase fija el código HTTP y un código de error estable
 * que se expone en la respuesta sin filtrar información interna.
 */
@Getter
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String codigo;

    protected ApiException(HttpStatus status, String codigo, String mensaje) {
        super(mensaje);
        this.status = status;
        this.codigo = codigo;
    }
}
