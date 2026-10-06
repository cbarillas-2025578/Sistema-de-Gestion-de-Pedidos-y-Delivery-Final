package com.sistemadelivery.main.mapper;

import com.sistemadelivery.main.dto.response.UsuarioResponse;
import com.sistemadelivery.main.entity.Usuario;
import org.springframework.stereotype.Component;

/** Nunca expone la contraseña ni su hash. */
@Component
public class UsuarioMapper {

    public UsuarioResponse aRespuesta(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getDireccion(),
                usuario.getTelefono(),
                usuario.getRol(),
                usuario.getActivo(),
                usuario.getFechaCreacion());
    }
}
