package com.sistemadelivery.main.exception;

import org.springframework.http.HttpStatus;

/** 404: recurso inexistente o no visible para el solicitante. */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String mensaje) {
        super(HttpStatus.NOT_FOUND, "RECURSO_NO_ENCONTRADO", mensaje);
    }

    public static ResourceNotFoundException de(String recurso, Long id) {
        return new ResourceNotFoundException(recurso + " con id " + id + " no existe");
    }
}
