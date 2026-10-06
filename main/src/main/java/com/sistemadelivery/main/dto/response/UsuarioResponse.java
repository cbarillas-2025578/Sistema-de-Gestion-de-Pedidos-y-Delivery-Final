package com.sistemadelivery.main.dto.response;

import com.sistemadelivery.main.entity.enums.Rol;

import java.time.LocalDateTime;

/** Nunca incluye la contraseña ni su hash. */
public record UsuarioResponse(Long id,
                              String nombre,
                              String email,
                              String direccion,
                              String telefono,
                              Rol rol,
                              Boolean activo,
                              LocalDateTime fechaCreacion) {
}
