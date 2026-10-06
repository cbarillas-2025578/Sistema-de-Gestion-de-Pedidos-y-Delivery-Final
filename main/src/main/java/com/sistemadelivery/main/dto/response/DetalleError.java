package com.sistemadelivery.main.dto.response;

/** Detalle de una validación fallida (campo + mensaje). */
public record DetalleError(String campo, String mensaje) {
}
