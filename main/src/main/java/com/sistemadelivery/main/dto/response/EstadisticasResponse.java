package com.sistemadelivery.main.dto.response;

import com.sistemadelivery.main.entity.enums.EstadoPedido;

import java.math.BigDecimal;
import java.util.Map;

/** Estadísticas básicas de pedidos y ventas para el panel administrativo. */
public record EstadisticasResponse(long totalPedidos,
                                   Map<EstadoPedido, Long> pedidosPorEstado,
                                   BigDecimal montoTotalVentas,
                                   BigDecimal ticketPromedio) {
}
