package com.sistemadelivery.main.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.sistemadelivery.main.validation.SinProductosDuplicados;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * Creación de pedido multiproducto de UN SOLO comercio.
 * No acepta precios, subtotales, costo de envío, monto total,
 * identificador de cliente ni de repartidor: todo se calcula/obtiene en el servidor.
 *
 * <p>{@code comercioId} es opcional: si no se envía, el comercio se deduce de los
 * productos del pedido (todas las líneas deben pertenecer al mismo comercio).
 * La lista de líneas se acepta como {@code productos} o como {@code items}.</p>
 */
@SinProductosDuplicados
public record PedidoRequest(
        @Positive(message = "El id del comercio debe ser positivo")
        Long comercioId,

        @NotEmpty(message = "El pedido debe incluir al menos un producto")
        @JsonAlias("items")
        @Valid
        List<ItemPedidoRequest> productos) {
}
