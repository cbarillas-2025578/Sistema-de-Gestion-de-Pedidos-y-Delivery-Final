package com.sistemadelivery.main.mapper;

import com.sistemadelivery.main.dto.response.ComercioResponse;
import com.sistemadelivery.main.entity.Comercio;
import org.springframework.stereotype.Component;

@Component
public class ComercioMapper {

    public ComercioResponse aRespuesta(Comercio comercio) {
        return new ComercioResponse(
                comercio.getId(),
                comercio.getNombre(),
                comercio.getCategoria(),
                comercio.getDireccion(),
                comercio.getAbierto(),
                comercio.getActivo(),
                comercio.getFechaCreacion());
    }
}
