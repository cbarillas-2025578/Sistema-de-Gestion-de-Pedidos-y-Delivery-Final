package com.sistemadelivery.main.dto.request;

/**
 * Cambio del estado operativo de un comercio.
 * Ambos campos son opcionales, pero al menos uno debe enviarse
 * (lo valida el servicio antes de aplicar el cambio).
 */
public record EstadoComercioRequest(Boolean activo, Boolean abierto) {
}
