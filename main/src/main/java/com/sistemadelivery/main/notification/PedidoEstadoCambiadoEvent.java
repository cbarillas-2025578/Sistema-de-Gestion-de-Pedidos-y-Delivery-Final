package com.sistemadelivery.main.notification;

import com.sistemadelivery.main.entity.enums.EstadoPedido;

import java.time.LocalDateTime;

/**
 * Evento de dominio publicado cuando un pedido cambia de estado.
 * Los suscriptores SSE lo reciben únicamente DESPUÉS de confirmar
 * la transacción (@TransactionalEventListener phase = AFTER_COMMIT),
 * de modo que nunca se notifica un cambio que se revierta.
 */
public record PedidoEstadoCambiadoEvent(Long pedidoId,
                                        EstadoPedido estado,
                                        String usuario,
                                        LocalDateTime fecha) {
}
