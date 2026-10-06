package com.sistemadelivery.main.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** Ajuste absoluto del stock (el valor anterior queda registrado en auditoría). */
public record StockRequest(
        @NotNull(message = "El stock es obligatorio")
        @PositiveOrZero(message = "El stock no puede ser negativo")
        Integer stock) {
}
