package com.sistemadelivery.main.util;

import com.sistemadelivery.main.entity.enums.EstadoPedido;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Máquina de estados del pedido (§6.4).
 * Toda transición distinta de las definidas aquí debe rechazarse.
 */
public final class TransicionesEstado {

    private TransicionesEstado() {
    }

    private static final Map<EstadoPedido, Set<EstadoPedido>> PERMITIDAS = Map.of(
            EstadoPedido.PENDIENTE, EnumSet.of(EstadoPedido.EN_PREPARACION, EstadoPedido.CANCELADO),
            EstadoPedido.EN_PREPARACION, EnumSet.of(EstadoPedido.EN_CAMINO),
            EstadoPedido.EN_CAMINO, EnumSet.of(EstadoPedido.ENTREGADO),
            EstadoPedido.ENTREGADO, EnumSet.noneOf(EstadoPedido.class),
            EstadoPedido.CANCELADO, EnumSet.noneOf(EstadoPedido.class));

    public static boolean esPermitida(EstadoPedido origen, EstadoPedido destino) {
        if (origen == null || destino == null) {
            return false;
        }
        return PERMITIDAS.getOrDefault(origen, Set.of()).contains(destino);
    }
}
