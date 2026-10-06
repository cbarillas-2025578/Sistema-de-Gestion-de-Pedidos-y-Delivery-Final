package com.sistemadelivery.main.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Envoltorio uniforme para todas las respuestas paginadas.
 * Uso: {@code PageResponse.de(pagina.map(mapper::aRespuesta))}.
 */
public record PageResponse<T>(List<T> contenido,
                              int pagina,
                              int tamano,
                              long totalElementos,
                              int totalPaginas,
                              boolean primera,
                              boolean ultima) {

    public static <T> PageResponse<T> de(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isFirst(), page.isLast());
    }
}
