package com.sistemadelivery.main.service.impl;

import com.sistemadelivery.main.entity.HistorialEstadoPedido;
import com.sistemadelivery.main.entity.Pedido;
import com.sistemadelivery.main.entity.enums.EstadoPedido;
import com.sistemadelivery.main.exception.ConflictException;
import com.sistemadelivery.main.notification.PedidoEstadoCambiadoEvent;
import com.sistemadelivery.main.repository.HistorialEstadoPedidoRepository;
import com.sistemadelivery.main.service.AuditoriaService;
import com.sistemadelivery.main.service.PedidoEstadoService;
import com.sistemadelivery.main.util.TransicionesEstado;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PedidoEstadoServiceImpl implements PedidoEstadoService {

    private final HistorialEstadoPedidoRepository historialRepository;
    private final AuditoriaService auditoriaService;
    private final ApplicationEventPublisher eventoPublisher;

    @Override
    @Transactional
    public void registrarCreacion(Pedido pedido, String actorEmail) {
        guardarHistorial(pedido, null, EstadoPedido.PENDIENTE, actorEmail);
        auditoriaService.registrar(actorEmail, "PEDIDO_CREAR", "Pedido", pedido.getId(),
                "Pedido creado con total " + pedido.getMontoTotal()
                        + " (" + pedido.getDetalles().size() + " línea(s))");
        publicar(pedido, actorEmail);
    }

    @Override
    @Transactional
    public void aplicar(Pedido pedido, EstadoPedido destino, String actorEmail) {
        EstadoPedido origen = pedido.getEstado();
        if (!TransicionesEstado.esPermitida(origen, destino)) {
            throw new ConflictException("TRANSICION_INVALIDA",
                    "Transición no permitida: " + origen + " → " + destino);
        }
        pedido.setEstado(destino);
        guardarHistorial(pedido, origen, destino, actorEmail);
        auditoriaService.registrar(actorEmail, "PEDIDO_ESTADO", "Pedido", pedido.getId(),
                origen + " → " + destino);
        publicar(pedido, actorEmail);
    }

    private void guardarHistorial(Pedido pedido, EstadoPedido anterior, EstadoPedido actual, String actorEmail) {
        historialRepository.save(HistorialEstadoPedido.builder()
                .pedido(pedido)
                .estadoAnterior(anterior)
                .estado(actual)
                .usuario(actorEmail)
                .build());
    }

    private void publicar(Pedido pedido, String actorEmail) {
        eventoPublisher.publishEvent(new PedidoEstadoCambiadoEvent(
                pedido.getId(), pedido.getEstado(), actorEmail, LocalDateTime.now()));
    }
}
