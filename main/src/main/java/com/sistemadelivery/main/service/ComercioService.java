package com.sistemadelivery.main.service;

import com.sistemadelivery.main.dto.request.ComercioRequest;
import com.sistemadelivery.main.dto.request.EstadoComercioRequest;
import com.sistemadelivery.main.dto.response.ComercioResponse;
import com.sistemadelivery.main.dto.response.PageResponse;
import com.sistemadelivery.main.entity.enums.CategoriaComercio;

public interface ComercioService {

    /** Catálogo público: comercios activos y abiertos. */
    PageResponse<ComercioResponse> listarAbiertos(int page, int size);

    /** Listado administrativo con filtros opcionales. */
    PageResponse<ComercioResponse> listarParaGestion(Boolean activo, Boolean abierto,
                                                     CategoriaComercio categoria, String q,
                                                     int page, int size);

    ComercioResponse obtener(Long id);

    ComercioResponse crear(ComercioRequest request);

    ComercioResponse actualizar(Long id, ComercioRequest request);

    ComercioResponse cambiarEstado(Long id, EstadoComercioRequest request);
}
