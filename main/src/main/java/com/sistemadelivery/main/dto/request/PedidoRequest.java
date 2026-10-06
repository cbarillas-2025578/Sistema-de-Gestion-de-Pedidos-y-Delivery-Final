package com.sistemadelivery.main.dto.request;

import com.sistemadelivery.main.validation.SinProductosDuplicados;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * Creación de pedido multiproducto de UN SOLO comercio.
 * No acepta precios, subtotales, costo de envío, monto total,
 * identificador de cliente ni de repartidor: todo se calcula/obtiene en el servidor.
 */
@SinProductosDuplicados
public record PedidoRequest(
        @NotNull(message = "El comercio es obligatorio")
        @Positive(message = "El id del comercio debe ser positivo")
        Long comercioId,

        @NotEmpty(message = "El pedido debe incluir al menos un producto")
        @Valid
        List<ItemPedidoRequest> productos) {
}
