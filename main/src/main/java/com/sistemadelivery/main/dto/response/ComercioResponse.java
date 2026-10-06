package com.sistemadelivery.main.dto.response;

import com.sistemadelivery.main.entity.enums.CategoriaComercio;

import java.time.LocalDateTime;

public record ComercioResponse(Long id,
                               String nombre,
                               CategoriaComercio categoria,
                               String direccion,
                               Boolean abierto,
                               Boolean activo,
                               LocalDateTime fechaCreacion) {
}
