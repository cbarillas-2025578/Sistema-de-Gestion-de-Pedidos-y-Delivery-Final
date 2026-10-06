package com.sistemadelivery.main.dto.request;

import com.sistemadelivery.main.entity.enums.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Alta de usuarios por parte del administrador.
 * Solo se permiten roles CLIENTE y REPARTIDOR: la creación de administradores
 * se realiza mediante el mecanismo de inicialización seguro (ADMIN_EMAIL/ADMIN_PASSWORD).
 */
public record UsuarioCrearRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
        String nombre,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato válido")
        @Size(max = 100, message = "El email no puede exceder 100 caracteres")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        String password,

        @NotNull(message = "El rol es obligatorio")
        Rol rol,

        @Size(max = 255, message = "La dirección no puede exceder 255 caracteres")
        String direccion,

        @Size(max = 20, message = "El teléfono no puede exceder 20 caracteres")
        String telefono) {
}
