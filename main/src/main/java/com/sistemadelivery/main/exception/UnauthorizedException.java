package com.sistemadelivery.main.exception;

import org.springframework.http.HttpStatus;

/** 401: credenciales ausentes, inválidas o token expirado. */
public class UnauthorizedException extends ApiException {

    public UnauthorizedException(String codigo, String mensaje) {
        super(HttpStatus.UNAUTHORIZED, codigo, mensaje);
    }
}
