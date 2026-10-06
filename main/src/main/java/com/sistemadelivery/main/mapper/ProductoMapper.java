package com.sistemadelivery.main.mapper;

import com.sistemadelivery.main.dto.response.ProductoResponse;
import com.sistemadelivery.main.entity.Producto;
import org.springframework.stereotype.Component;

/**
 * Accede a las asociaciones (comercio/categoría) con carga perezosa:
 * debe invocarse dentro de un método transaccional del servicio.
 */
@Component
public class ProductoMapper {

    public ProductoResponse aRespuesta(Producto producto) {
        return new ProductoResponse(
                producto.getId(),
                producto.getComercio() != null ? producto.getComercio().getId() : null,
                producto.getComercio() != null ? producto.getComercio().getNombre() : null,
                producto.getCategoria() != null ? producto.getCategoria().getId() : null,
                producto.getCategoria() != null ? producto.getCategoria().getNombre() : null,
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getPrecio(),
                producto.getStock(),
                producto.getDisponible(),
                producto.getFechaActualizacion());
    }
}
