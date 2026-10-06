package com.sistemadelivery.main.dto.request;

import jakarta.validation.constraints.NotNull;

/** Cambio de estado (activo/inactivo) de un recurso administrativo. */
public record EstadoRequest(
        @NotNull(message = "El estado 'activo' es obligatorio")
        Boolean activo) {
}
