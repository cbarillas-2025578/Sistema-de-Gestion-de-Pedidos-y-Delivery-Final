package com.sistemadelivery.main.service;

import com.sistemadelivery.main.dto.request.UsuarioCrearRequest;
import com.sistemadelivery.main.dto.response.PageResponse;
import com.sistemadelivery.main.dto.response.UsuarioResponse;
import com.sistemadelivery.main.entity.enums.Rol;

public interface UsuarioService {

    /** Consulta administrativa de usuarios (filtro opcional por rol). */
    PageResponse<UsuarioResponse> listar(Rol rol, int page, int size);

    /**
     * Alta de usuarios por el administrador.
     * Solo roles CLIENTE y REPARTIDOR: los administradores se crean
     * mediante el mecanismo de inicialización seguro.
     */
    UsuarioResponse crear(UsuarioCrearRequest request);
}
