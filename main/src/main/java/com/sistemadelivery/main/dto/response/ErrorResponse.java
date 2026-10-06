package com.sistemadelivery.main.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Formato único de error para toda la API.
 * Nunca incluye trazas, contraseñas, tokens ni detalle interno del servidor.
 */
public record ErrorResponse(String codigo,
                            String mensaje,
                            LocalDateTime fecha,
                            List<DetalleError> detalles) {

    public static ErrorResponse simple(String codigo, String mensaje) {
        return new ErrorResponse(codigo, mensaje, LocalDateTime.now(), null);
    }

    public static ErrorResponse conDetalles(String codigo, String mensaje, List<DetalleError> detalles) {
        return new ErrorResponse(codigo, mensaje, LocalDateTime.now(), detalles);
    }
}
