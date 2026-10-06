package com.sistemadelivery.main.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementación en memoria (una sola instancia del servidor).
 * Para despliegues multi-instancia debe sustituirse por un canal compartido
 * (p. ej. Redis pub/sub) manteniendo la misma interfaz.
 */
@Slf4j
@Service
public class NotificacionServiceImpl implements NotificacionService {

    /** Suscripciones vivas por pedido. */
    private final Map<Long, List<SseEmitter>> suscriptores = new ConcurrentHashMap<>();

    private static final long TIMEOUT_MS = 30 * 60 * 1000L; // 30 minutos

    @Override
    public SseEmitter suscribir(Long pedidoId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        suscriptores.computeIfAbsent(pedidoId, clave -> Collections.synchronizedList(new ArrayList<>()))
                .add(emitter);

        Runnable limpiar = () -> remover(pedidoId, emitter);
        emitter.onCompletion(limpiar);
        emitter.onTimeout(limpiar);
        emitter.onError(evento -> limpiar.run());

        try {
            emitter.send(SseEmitter.event()
                    .name("conexion")
                    .data(Map.of("pedidoId", pedidoId, "mensaje", "Suscrito al seguimiento del pedido")));
        } catch (IOException | IllegalStateException e) {
            remover(pedidoId, emitter);
        }
        return emitter;
    }

    /** Se ejecuta solo si la transacción que publicó el evento confirmó el commit. */
    @Override
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void notificar(PedidoEstadoCambiadoEvent evento) {
        List<SseEmitter> emitters = suscriptores.get(evento.pedidoId());
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        Map<String, Object> datos = Map.of(
                "pedidoId", evento.pedidoId(),
                "estado", evento.estado().name(),
                "usuario", evento.usuario(),
                "fecha", String.valueOf(evento.fecha()));

        for (SseEmitter emitter : List.copyOf(emitters)) {
            try {
                emitter.send(SseEmitter.event().name("estado").data(datos));
            } catch (Exception e) {
                log.debug("Eliminando suscripción SSE caída para pedido {}", evento.pedidoId());
                remover(evento.pedidoId(), emitter);
            }
        }
    }

    private void remover(Long pedidoId, SseEmitter emitter) {
        suscriptores.computeIfPresent(pedidoId, (clave, lista) -> {
            lista.remove(emitter);
            return lista.isEmpty() ? null : lista;
        });
    }
}
