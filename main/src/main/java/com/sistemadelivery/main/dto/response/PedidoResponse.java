package com.sistemadelivery.main.dto.response;

import com.sistemadelivery.main.entity.enums.EstadoPedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoResponse(Long id,
                             Long clienteId,
                             String clienteNombre,
                             Long repartidorId,
                             String repartidorNombre,
                             Long comercioId,
                             String comercioNombre,
                             LocalDateTime fechaPedido,
                             BigDecimal costoEnvio,
                             BigDecimal montoTotal,
                             EstadoPedido estado,
                             List<DetallePedidoResponse> detalles,
                             Long version) {
}
