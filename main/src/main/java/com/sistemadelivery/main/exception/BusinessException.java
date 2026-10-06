package com.sistemadelivery.main.exception;

import org.springframework.http.HttpStatus;

/** 400: la solicitud viola una regla de negocio o llega con datos inválidos. */
public class BusinessException extends ApiException {

    public BusinessException(String codigo, String mensaje) {
        super(HttpStatus.BAD_REQUEST, codigo, mensaje);
    }

    public static BusinessException de(String mensaje) {
        return new BusinessException("REGLA_NEGOCIO", mensaje);
    }
}
