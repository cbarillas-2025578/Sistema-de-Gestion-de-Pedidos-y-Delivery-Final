package com.sistemadelivery.main.dto.response;

public record CategoriaResponse(Long id,
                                String nombre,
                                String descripcion,
                                Boolean activo) {
}
