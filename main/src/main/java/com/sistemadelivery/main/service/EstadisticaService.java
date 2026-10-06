package com.sistemadelivery.main.service;

import com.sistemadelivery.main.dto.response.EstadisticasResponse;

public interface EstadisticaService {

    /** Estadísticas básicas de pedidos y ventas para el panel administrativo. */
    EstadisticasResponse obtener();
}
