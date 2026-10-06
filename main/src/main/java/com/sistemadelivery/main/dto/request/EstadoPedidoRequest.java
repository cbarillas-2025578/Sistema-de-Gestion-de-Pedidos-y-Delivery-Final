package com.sistemadelivery.main.dto.request;

import com.sistemadelivery.main.entity.enums.EstadoPedido;
import jakarta.validation.constraints.NotNull;

/** Solicitud de transición de estado (sujeta a la máquina de estados del dominio). */
public record EstadoPedidoRequest(
        @NotNull(message = "El estado es obligatorio")
        EstadoPedido estado) {
}
