package com.sistemadelivery.main.dto.response;

import com.sistemadelivery.main.entity.enums.Rol;

/** Respuesta de autenticación: el token solo se emite una vez aquí. */
public record AuthResponse(String token,
                           String tipo,
                           long expiracionMs,
                           Long id,
                           String nombre,
                           String email,
                           Rol rol) {

    public static AuthResponse de(String token, long expiracionMs, Long id, String nombre, String email, Rol rol) {
        return new AuthResponse(token, "Bearer", expiracionMs, id, nombre, email, rol);
    }
}
