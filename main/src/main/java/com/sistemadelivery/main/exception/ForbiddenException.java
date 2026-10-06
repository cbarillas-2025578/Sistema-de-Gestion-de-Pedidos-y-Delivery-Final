package com.sistemadelivery.main.exception;

import org.springframework.http.HttpStatus;

/** 403: autenticado pero sin permiso sobre el recurso o la operación. */
public class ForbiddenException extends ApiException {

    public ForbiddenException(String mensaje) {
        super(HttpStatus.FORBIDDEN, "ACCESO_DENEGADO", mensaje);
    }
}
