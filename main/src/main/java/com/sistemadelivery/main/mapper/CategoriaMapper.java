package com.sistemadelivery.main.mapper;

import com.sistemadelivery.main.dto.response.CategoriaResponse;
import com.sistemadelivery.main.entity.Categoria;
import org.springframework.stereotype.Component;

@Component
public class CategoriaMapper {

    public CategoriaResponse aRespuesta(Categoria categoria) {
        return new CategoriaResponse(
                categoria.getId(),
                categoria.getNombre(),
                categoria.getDescripcion(),
                categoria.getActivo());
    }
}
