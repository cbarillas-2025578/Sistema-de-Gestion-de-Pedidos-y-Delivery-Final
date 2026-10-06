package com.sistemadelivery.main.service;

import com.sistemadelivery.main.dto.request.EstadoPedidoRequest;
import com.sistemadelivery.main.dto.response.PageResponse;
import com.sistemadelivery.main.dto.response.PedidoResponse;

public interface RepartidorService {

    /** Pedidos preparados y aún sin repartidor (elegibles para aceptar). */
    PageResponse<PedidoResponse> pedidosDisponibles(int page, int size);

    /** Pedidos asignados al repartidor autenticado. */
    PageResponse<PedidoResponse> misPedidos(int page, int size);

    /** Aceptación atómica: solo un repartidor puede asignar el mismo pedido. */
    PedidoResponse aceptar(Long pedidoId);

    /** Actualiza el estado de entrega respetando la máquina de estados. */
    PedidoResponse actualizarEstado(Long pedidoId, EstadoPedidoRequest request);
}
