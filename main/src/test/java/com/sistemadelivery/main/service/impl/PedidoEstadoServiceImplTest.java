package com.sistemadelivery.main.service.impl;

import com.sistemadelivery.main.entity.HistorialEstadoPedido;
import com.sistemadelivery.main.entity.Pedido;
import com.sistemadelivery.main.entity.enums.EstadoPedido;
import com.sistemadelivery.main.exception.ConflictException;
import com.sistemadelivery.main.notification.PedidoEstadoCambiadoEvent;
import com.sistemadelivery.main.repository.HistorialEstadoPedidoRepository;
import com.sistemadelivery.main.service.AuditoriaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Pruebas unitarias de PedidoEstadoServiceImpl:
 * cada transición válida persiste el historial, audita y publica la notificación;
 * las transiciones inválidas se rechazan sin escribir nada.
 */
@ExtendWith(MockitoExtension.class)
class PedidoEstadoServiceImplTest {

    @Mock
    private HistorialEstadoPedidoRepository historialRepository;
    @Mock
    private AuditoriaService auditoriaService;
    @Mock
    private ApplicationEventPublisher eventoPublisher;

    private PedidoEstadoServiceImpl pedidoEstadoService;

    @BeforeEach
    void setUp() {
        pedidoEstadoService = new PedidoEstadoServiceImpl(historialRepository, auditoriaService, eventoPublisher);
    }

    @Test
    void registrarCreacion_persisteHistorialAuditaYPublica() {
        Pedido pedido = Pedido.builder().id(1L).estado(EstadoPedido.PENDIENTE).build();

        pedidoEstadoService.registrarCreacion(pedido, "cliente@entrega.com");

        ArgumentCaptor<HistorialEstadoPedido> historial = ArgumentCaptor.forClass(HistorialEstadoPedido.class);
        verify(historialRepository).save(historial.capture());
        assertThat(historial.getValue().getEstado()).isEqualTo(EstadoPedido.PENDIENTE);
        assertThat(historial.getValue().getEstadoAnterior()).isNull();
        assertThat(historial.getValue().getUsuario()).isEqualTo("cliente@entrega.com");

        verify(auditoriaService).registrar(eq("cliente@entrega.com"), eq("PEDIDO_CREAR"), eq("Pedido"),
                eq(1L), anyString());
        verify(eventoPublisher).publishEvent(any(PedidoEstadoCambiadoEvent.class));
    }

    @Test
    void aplicar_transicionValidaCambiaEstadoYRegistra() {
        Pedido pedido = Pedido.builder().id(2L).estado(EstadoPedido.PENDIENTE).build();

        pedidoEstadoService.aplicar(pedido, EstadoPedido.EN_PREPARACION, "admin@entrega.com");

        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.EN_PREPARACION);

        ArgumentCaptor<HistorialEstadoPedido> historial = ArgumentCaptor.forClass(HistorialEstadoPedido.class);
        verify(historialRepository).save(historial.capture());
        assertThat(historial.getValue().getEstadoAnterior()).isEqualTo(EstadoPedido.PENDIENTE);
        assertThat(historial.getValue().getEstado()).isEqualTo(EstadoPedido.EN_PREPARACION);

        verify(auditoriaService).registrar(eq("admin@entrega.com"), eq("PEDIDO_ESTADO"), eq("Pedido"),
                eq(2L), eq("PENDIENTE → EN_PREPARACION"));
        verify(eventoPublisher).publishEvent(any(PedidoEstadoCambiadoEvent.class));
    }

    @Test
    void aplicar_rechazaTransicionInvalidaSinEscribirNada() {
        Pedido pedido = Pedido.builder().id(3L).estado(EstadoPedido.PENDIENTE).build();

        assertThatThrownBy(() -> pedidoEstadoService.aplicar(pedido, EstadoPedido.ENTREGADO, "admin@entrega.com"))
                .isInstanceOf(ConflictException.class)
                .satisfies(e -> assertThat(((ConflictException) e).getCodigo()).isEqualTo("TRANSICION_INVALIDA"));

        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.PENDIENTE); // sin cambios
        verify(historialRepository, never()).save(any());
        verify(auditoriaService, never()).registrar(anyString(), anyString(), anyString(), any(), anyString());
        verify(eventoPublisher, never()).publishEvent(any());
    }

    @Test
    void aplicar_noPermiteSalirDeEstadosFinales() {
        Pedido cancelado = Pedido.builder().id(4L).estado(EstadoPedido.CANCELADO).build();
        assertThatThrownBy(() -> pedidoEstadoService.aplicar(cancelado, EstadoPedido.PENDIENTE, "sistema"))
                .isInstanceOf(ConflictException.class);
        verify(historialRepository, never()).save(any());

        Pedido entregado = Pedido.builder().id(5L).estado(EstadoPedido.ENTREGADO).build();
        assertThatThrownBy(() -> pedidoEstadoService.aplicar(entregado, EstadoPedido.EN_CAMINO, "sistema"))
                .isInstanceOf(ConflictException.class);
        verify(historialRepository, never()).save(any());
    }
}