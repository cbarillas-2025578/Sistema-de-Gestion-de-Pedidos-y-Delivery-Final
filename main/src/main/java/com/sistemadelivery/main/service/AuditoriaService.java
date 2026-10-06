package com.sistemadelivery.main.service;

import com.sistemadelivery.main.dto.response.AuditoriaResponse;
import com.sistemadelivery.main.dto.response.PageResponse;

import java.time.LocalDateTime;

public interface AuditoriaService {

    /** Registra un evento con el usuario explícito (inicio de registro, etc.). */
    void registrar(String usuario, String accion, String entidad, Long entidadId, String detalles);

    /** Registra un evento atribuyéndolo al usuario autenticado en el contexto de seguridad. */
    void registrar(String accion, String entidad, Long entidadId, String detalles);

    /** Consulta administrativa con filtros combinables y paginación. */
    PageResponse<AuditoriaResponse> buscar(String usuario, String accion, String entidad, Long entidadId,
                                           LocalDateTime desde, LocalDateTime hasta, int page, int size);
}
