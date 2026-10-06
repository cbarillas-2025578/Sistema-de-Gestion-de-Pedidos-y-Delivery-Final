package com.sistemadelivery.main.dto.request;

import jakarta.validation.constraints.NotNull;

public record DisponibilidadRequest(
        @NotNull(message = "La disponibilidad es obligatoria")
        Boolean disponible) {
}
