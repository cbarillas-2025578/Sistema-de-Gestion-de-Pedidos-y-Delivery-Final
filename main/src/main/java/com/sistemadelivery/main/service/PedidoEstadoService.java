package com.sistemadelivery.main.service;

import com.sistemadelivery.main.entity.Pedido;
import com.sistemadelivery.main.entity.enums.EstadoPedido;

/**
 * Coordina los cambios de estado del pedido: valida la máquina de estados,
 * persiste el historial, registra la auditoría y publica el evento
 * de notificación (que se notifica tras el commit).
 */
public interface PedidoEstadoService {

    /** Registra el estado inicial al crear un pedido. */
    void registrarCreacion(Pedido pedido, String actorEmail);

    /**
     * Valida la transición contra la máquina de estados y la aplica.
     * Lanza {@code ConflictException} si la transición no está permitida.
     */
    void aplicar(Pedido pedido, EstadoPedido destino, String actorEmail);
}
