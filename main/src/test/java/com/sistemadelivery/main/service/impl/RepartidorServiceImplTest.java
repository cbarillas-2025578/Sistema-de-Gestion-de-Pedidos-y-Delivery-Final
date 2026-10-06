package com.sistemadelivery.main.service.impl;

import com.sistemadelivery.main.dto.request.EstadoPedidoRequest;
import com.sistemadelivery.main.entity.Comercio;
import com.sistemadelivery.main.entity.Pedido;
import com.sistemadelivery.main.entity.Usuario;
import com.sistemadelivery.main.entity.enums.CategoriaComercio;
import com.sistemadelivery.main.entity.enums.EstadoPedido;
import com.sistemadelivery.main.entity.enums.Rol;
import com.sistemadelivery.main.exception.ConflictException;
import com.sistemadelivery.main.exception.ForbiddenException;
import com.sistemadelivery.main.mapper.PedidoMapper;
import com.sistemadelivery.main.repository.PedidoRepository;
import com.sistemadelivery.main.repository.UsuarioRepository;
import com.sistemadelivery.main.service.AuditoriaService;
import com.sistemadelivery.main.service.PedidoEstadoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de RepartidorServiceImpl:
 * - asignación atómica (con llave pesada) de un pedido en EN_PREPARACION;
 * - pedidos ya asignados → 409;
 * - un repartidor solo puede ejecutar EN_CAMINO o ENTREGADO sobre sus pedidos.
 */
@ExtendWith(MockitoExtension.class)
class RepartidorServiceImplTest {

    private static final String EMAIL_REPARTIDOR = "repartidor@entrega.com";

    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PedidoMapper mapper;
    @Mock
    private PedidoEstadoService pedidoEstadoService;
    @Mock
    private AuditoriaService auditoriaService;

    private RepartidorServiceImpl repartidorService;
    private Usuario repartidor;

    @BeforeEach
    void setUp() {
        repartidorService = new RepartidorServiceImpl(pedidoRepository, usuarioRepository, mapper,
                pedidoEstadoService, auditoriaService);
        repartidor = Usuario.builder().id(3L).email(EMAIL_REPARTIDOR).nombre("Repartidor")
                .rol(Rol.REPARTIDOR).activo(true).build();
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private void autenticar(String email, Rol rol) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()))));
    }

    private Pedido pedidoEnPreparacionSinRepartidor(Long id) {
        Comercio comercio = Comercio.builder().id(10L).nombre("Pollería").categoria(CategoriaComercio.RESTAURANTE)
                .direccion("Z1").abierto(true).activo(true).build();
        return Pedido.builder().id(id).comercio(comercio).estado(EstadoPedido.EN_PREPARACION)
                .costoEnvio(new BigDecimal("20.00")).montoTotal(new BigDecimal("71.00")).build();
    }

    @Test
    void aceptar_asignaElPedidoAlRepartidorAutenticado() {
        autenticar(EMAIL_REPARTIDOR, Rol.REPARTIDOR);
        when(usuarioRepository.findByEmail(EMAIL_REPARTIDOR)).thenReturn(Optional.of(repartidor));

        Pedido pedido = pedidoEnPreparacionSinRepartidor(70L);
        when(pedidoRepository.findByIdForUpdate(70L)).thenReturn(Optional.of(pedido));

        repartidorService.aceptar(70L);

        assertThat(pedido.getRepartidor()).isSameAs(repartidor);
        verify(auditoriaService).registrar(eq(EMAIL_REPARTIDOR), eq("PEDIDO_ASIGNACION"), eq("Pedido"),
                eq(70L), anyString());
    }

    @Test
    void aceptar_rechazaPedidoYaAsignado() {
        autenticar(EMAIL_REPARTIDOR, Rol.REPARTIDOR);
        when(usuarioRepository.findByEmail(EMAIL_REPARTIDOR)).thenReturn(Optional.of(repartidor));

        Pedido pedido = pedidoEnPreparacionSinRepartidor(71L);
        pedido.setRepartidor(Usuario.builder().id(4L).email("otro@entrega.com").rol(Rol.REPARTIDOR).build());
        when(pedidoRepository.findByIdForUpdate(71L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> repartidorService.aceptar(71L))
                .isInstanceOf(ConflictException.class)
                .satisfies(e -> assertThat(((ConflictException) e).getCodigo()).isEqualTo("PEDIDO_ASIGNADO"));

        assertThat(pedido.getRepartidor()).isNotSameAs(repartidor);
    }

    @Test
    void aceptar_rechazaPedidoNoDisponible() {
        autenticar(EMAIL_REPARTIDOR, Rol.REPARTIDOR);
        when(usuarioRepository.findByEmail(EMAIL_REPARTIDOR)).thenReturn(Optional.of(repartidor));

        Pedido pedido = pedidoEnPreparacionSinRepartidor(72L);
        pedido.setEstado(EstadoPedido.PENDIENTE);
        when(pedidoRepository.findByIdForUpdate(72L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> repartidorService.aceptar(72L))
                .isInstanceOf(ConflictException.class)
                .satisfies(e -> assertThat(((ConflictException) e).getCodigo())
                        .isEqualTo("PEDIDO_NO_DISPONIBLE"));
    }

    @Test
    void aceptar_rechazaUsuarioQueNoEsRepartidor() {
        autenticar("cliente@entrega.com", Rol.CLIENTE);
        Usuario cliente = Usuario.builder().id(5L).email("cliente@entrega.com").rol(Rol.CLIENTE).build();
        when(usuarioRepository.findByEmail("cliente@entrega.com")).thenReturn(Optional.of(cliente));

        assertThatThrownBy(() -> repartidorService.aceptar(70L))
                .isInstanceOf(ForbiddenException.class);

        verify(pedidoRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void actualizarEstado_repardidorAsignadoEjecutaEnCamino() {
        autenticar(EMAIL_REPARTIDOR, Rol.REPARTIDOR);

        Pedido pedido = pedidoEnPreparacionSinRepartidor(80L);
        pedido.setRepartidor(repartidor);
        when(pedidoRepository.findByIdForUpdate(80L)).thenReturn(Optional.of(pedido));

        repartidorService.actualizarEstado(80L, new EstadoPedidoRequest(EstadoPedido.EN_CAMINO));

        verify(pedidoEstadoService).aplicar(pedido, EstadoPedido.EN_CAMINO, EMAIL_REPARTIDOR);
    }

    @Test
    void actualizarEstado_rechazaPedidoNoAsignadoAEsteRepartidor() {
        autenticar(EMAIL_REPARTIDOR, Rol.REPARTIDOR);

        Pedido pedido = pedidoEnPreparacionSinRepartidor(81L);
        pedido.setRepartidor(Usuario.builder().id(9L).email("otro@entrega.com").rol(Rol.REPARTIDOR).build());
        when(pedidoRepository.findByIdForUpdate(81L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> repartidorService.actualizarEstado(
                81L, new EstadoPedidoRequest(EstadoPedido.ENTREGADO)))
                .isInstanceOf(ForbiddenException.class);

        verify(pedidoEstadoService, never()).aplicar(any(), any(), anyString());
    }

    @Test
    void actualizarEstado_rechazaEstadosFueraDelAlcanceDelRepartidor() {
        autenticar(EMAIL_REPARTIDOR, Rol.REPARTIDOR);

        Pedido pedido = pedidoEnPreparacionSinRepartidor(82L);
        pedido.setRepartidor(repartidor);
        when(pedidoRepository.findByIdForUpdate(82L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> repartidorService.actualizarEstado(
                82L, new EstadoPedidoRequest(EstadoPedido.PENDIENTE)))
                .isInstanceOf(ForbiddenException.class);

        verify(pedidoEstadoService, never()).aplicar(any(), any(), anyString());
    }
}