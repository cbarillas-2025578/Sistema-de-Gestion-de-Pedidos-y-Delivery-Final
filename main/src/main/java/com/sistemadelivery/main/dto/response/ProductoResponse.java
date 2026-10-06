package com.sistemadelivery.main.dto.response;

import java.time.LocalDateTime;
import java.math.BigDecimal;

public record ProductoResponse(Long id,
                               Long comercioId,
                               String comercioNombre,
                               Long categoriaId,
                               String categoriaNombre,
                               String nombre,
                               String descripcion,
                               BigDecimal precio,
                               Integer stock,
                               Boolean disponible,
                               LocalDateTime fechaActualizacion) {
}
