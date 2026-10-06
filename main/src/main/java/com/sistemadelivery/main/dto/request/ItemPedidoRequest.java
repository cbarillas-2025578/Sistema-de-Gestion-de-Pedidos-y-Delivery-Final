package com.sistemadelivery.main.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** El cliente envía únicamente identificador y cantidad: precios y totales se calculan en el servidor. */
public record ItemPedidoRequest(
        @NotNull(message = "El id del producto es obligatorio")
        @Positive(message = "El id del producto debe ser positivo")
        Long productoId,

        @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "La cantidad debe ser un entero positivo")
        Integer cantidad) {
}
