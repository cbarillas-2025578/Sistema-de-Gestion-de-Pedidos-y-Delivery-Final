package com.sistemadelivery.main.service;

import com.sistemadelivery.main.dto.request.EstadoPedidoRequest;
import com.sistemadelivery.main.dto.request.PedidoRequest;
import com.sistemadelivery.main.dto.response.HistorialEstadoResponse;
import com.sistemadelivery.main.dto.response.PageResponse;
import com.sistemadelivery.main.dto.response.PedidoResponse;
import com.sistemadelivery.main.entity.enums.EstadoPedido;

import java.time.LocalDateTime;
import java.util.List;

public interface PedidoService {

    /** Crea un pedido multiproducto de un solo comercio con control transaccional de stock. */
    PedidoResponse crear(PedidoRequest request);

    /** Pedidos del cliente autenticado. */
    PageResponse<PedidoResponse> misPedidos(int page, int size);

    /** Detalle con verificación de pertenencia (dueño, repartidor asignado o admin). */
    PedidoResponse obtener(Long id);

    /** Cancela un pedido propio en estado PENDIENTE y devuelve el stock una única vez. */
    PedidoResponse cancelar(Long id);

    /**
     * Transición administrativa: única permitida es PENDIENTE → EN_PREPARACION
     * (proceso interno que en una futura versión correspondería al comercio).
     */
    PedidoResponse cambiarEstadoAdministrativo(Long id, EstadoPedidoRequest request);

    /** Listado administrativo con filtros y paginación. */
    PageResponse<PedidoResponse> listarTodos(EstadoPedido estado, Long clienteId, Long repartidorId,
                                             LocalDateTime desde, LocalDateTime hasta, int page, int size);

    /** Historial de cambios de estado de un pedido (acceso verificado). */
    List<HistorialEstadoResponse> historial(Long id);
}
