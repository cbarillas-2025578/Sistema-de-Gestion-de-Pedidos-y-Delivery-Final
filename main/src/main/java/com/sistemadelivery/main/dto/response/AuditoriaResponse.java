package com.sistemadelivery.main.dto.response;

import java.time.LocalDateTime;

public record AuditoriaResponse(Long id,
                                String usuario,
                                String accion,
                                String entidad,
                                Long entidadId,
                                String detalles,
                                LocalDateTime fecha) {
}
