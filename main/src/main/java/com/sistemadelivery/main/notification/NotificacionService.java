package com.sistemadelivery.main.notification;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Seguimiento en tiempo real de pedidos mediante Server-Sent Events.
 * El alta y la verificación de pertenencia del pedido los realiza el controlador
 * (solo el cliente dueño, el repartidor asignado o un administrador pueden suscribirse).
 */
public interface NotificacionService {

    /** Suscribe al cliente autenticado al seguimiento de un pedido. */
    SseEmitter suscribir(Long pedidoId);

    /** Publica el cambio de estado a los suscriptores (se ejecuta tras el commit). */
    void notificar(PedidoEstadoCambiadoEvent evento);
}
