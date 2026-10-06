package com.sistemadelivery.main.service;

import com.sistemadelivery.main.dto.request.DisponibilidadRequest;
import com.sistemadelivery.main.dto.request.ProductoRequest;
import com.sistemadelivery.main.dto.request.StockRequest;
import com.sistemadelivery.main.dto.response.PageResponse;
import com.sistemadelivery.main.dto.response.ProductoResponse;

public interface ProductoService {

    /**
     * Catálogo público (disponibles con stock) o listado de gestión
     * completo para administradores cuando {@code gestion} es true.
     */
    PageResponse<ProductoResponse> listarPorComercio(Long comercioId, boolean gestion, int page, int size);

    ProductoResponse obtener(Long id);

    ProductoResponse crear(Long comercioId, ProductoRequest request);

    ProductoResponse actualizar(Long id, ProductoRequest request);

    /** Ajuste absoluto de stock, auditado con el valor anterior. */
    ProductoResponse ajustarStock(Long id, StockRequest request);

    /** Activa o desactiva la disponibilidad del producto. */
    void cambiarDisponibilidad(Long id, DisponibilidadRequest request);
}
