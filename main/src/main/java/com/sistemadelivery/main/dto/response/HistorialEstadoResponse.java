package com.sistemadelivery.main.dto.response;

import com.sistemadelivery.main.entity.enums.EstadoPedido;

import java.time.LocalDateTime;

public record HistorialEstadoResponse(Long id,
                                      EstadoPedido estado,
                                      EstadoPedido estadoAnterior,
                                      String usuario,
                                      LocalDateTime fecha) {
}
