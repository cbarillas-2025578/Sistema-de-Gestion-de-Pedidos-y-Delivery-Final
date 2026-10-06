package com.sistemadelivery.main.service;

import com.sistemadelivery.main.dto.request.CategoriaRequest;
import com.sistemadelivery.main.dto.response.CategoriaResponse;

import java.util.List;

public interface CategoriaService {

    /** Categorías globales activas (catálogo público). */
    List<CategoriaResponse> listarActivas();

    CategoriaResponse crear(CategoriaRequest request);

    CategoriaResponse actualizar(Long id, CategoriaRequest request);

    CategoriaResponse cambiarEstado(Long id, boolean activo);
}
