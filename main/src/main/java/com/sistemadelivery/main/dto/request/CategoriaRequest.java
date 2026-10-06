package com.sistemadelivery.main.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Categoría global de productos (no confundir con la categoría del comercio). */
public record CategoriaRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 50, message = "El nombre no puede exceder 50 caracteres")
        String nombre,

        @Size(max = 255, message = "La descripción no puede exceder 255 caracteres")
        String descripcion) {
}
