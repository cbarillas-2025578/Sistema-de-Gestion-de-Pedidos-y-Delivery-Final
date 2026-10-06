package com.sistemadelivery.main.service.impl;

import com.sistemadelivery.main.dto.request.EstadoPedidoRequest;
import com.sistemadelivery.main.dto.request.ItemPedidoRequest;
import com.sistemadelivery.main.dto.request.PedidoRequest;
import com.sistemadelivery.main.entity.Comercio;
import com.sistemadelivery.main.entity.DetallePedido;
import com.sistemadelivery.main.entity.Pedido;
import com.sistemadelivery.main.entity.Producto;
import com.sistemadelivery.main.entity.Usuario;
import com.sistemadelivery.main.entity.enums.CategoriaComercio;
import com.sistemadelivery.main.entity.enums.EstadoPedido;
import com.sistemadelivery.main.entity.enums.Rol;
import com.sistemadelivery.main.exception.BusinessException;
import com.sistemadelivery.main.exception.ConflictException;
import com.sistemadelivery.main.exception.ForbiddenException;
import com.sistemadelivery.main.mapper.PedidoMapper;
import com.sistemadelivery.main.repository.ComercioRepository;
import com.sistemadelivery.main.repository.DetallePedidoRepository;
import com.sistemadelivery.main.repository.HistorialEstadoPedidoRepository;
import com.sistemadelivery.main.repository.PedidoRepository;
import com.sistemadelivery.main.repository.ProductoRepository;
import com.sistemadelivery.main.repository.UsuarioRepository;
import com.sistemadelivery.main.service.AuditoriaService;
import com.sistemadelivery.main.service.PedidoEstadoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de PedidoServiceImpl:
 * - totales calculados en el servidor (subtotales + envío fijo Q20.00);
 * - stock con bloqueo y devolución de stock exactamente una vez al cancelar;
 * - reglas de negocio: producto de otro comercio, stock insuficiente, duplicados;
 * - transición administrativa PENDIENTE → EN_PREPARACION.
 */
@ExtendWith(MockitoExtension.class)
class PedidoServiceImplTest {

    private static final String EMAIL_CLIENTE = "cliente@entrega.com";

    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private ComercioRepository comercioRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private DetallePedidoRepository detallePedidoRepository;
    @Mock
    private HistorialEstadoPedidoRepository historialRepository;
    @Mock
    private PedidoMapper mapper;
    @Mock
    private PedidoEstadoService pedidoEstadoService;
    @Mock
    private AuditoriaService auditoriaService;

    private PedidoServiceImpl pedidoService;

    private Usuario cliente;
    private Comercio comercio;
    private Producto pollo;
    private Producto bebida;

    @BeforeEach
    void setUp() {
        pedidoService = new PedidoServiceImpl(pedidoRepository, productoRepository, comercioRepository,
                usuarioRepository, detallePedidoRepository, historialRepository, mapper,
                pedidoEstadoService, auditoriaService);

        cliente = Usuario.builder().id(1L).email(EMAIL_CLIENTE).nombre("Cliente").rol(Rol.CLIENTE).activo(true).build();
        comercio = Comercio.builder().id(10L).nombre("Pollería Central").categoria(CategoriaComercio.RESTAURANTE)
                .direccion("Zona 1").abierto(true).activo(true).build();
        pollo = Producto.builder().id(100L).comercio(comercio).nombre("Pollo 1/4").precio(new BigDecimal("25.50"))
                .stock(10).disponible(true).build();
        bebida = Producto.builder().id(101L).comercio(comercio).nombre("Agua 600ml").precio(new BigDecimal("10.00"))
                .stock(5).disponible(true).build();
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private void autenticar(String email, Rol... roles) {
        List<SimpleGrantedAuthority> autoridades = java.util.Arrays.stream(roles)
                .map(rol -> new SimpleGrantedAuthority("ROLE_" + rol.name()))
                .toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null, autoridades));
    }

    // ------------------------------------------------------------------
    // Creación de pedido
    // ------------------------------------------------------------------

    @Test
    void crear_calculaSubtotalesEnvioFijoYTotalEnElServidor() {
        autenticar(EMAIL_CLIENTE, Rol.CLIENTE);
        when(usuarioRepository.findByEmail(EMAIL_CLIENTE)).thenReturn(Optional.of(cliente));
        when(comercioRepository.findById(10L)).thenReturn(Optional.of(comercio));
        when(productoRepository.findAllByIdInForUpdateOrderByIdAsc(anyList())).thenReturn(List.of(pollo, bebida));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        PedidoRequest request = new PedidoRequest(10L, List.of(
                new ItemPedidoRequest(100L, 2),
                new ItemPedidoRequest(101L, 1)));

        pedidoService.crear(request);

        ArgumentCaptor<Pedido> captor = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepository).save(captor.capture());
        Pedido guardado = captor.getValue();

        // 25.50 x 2 = 51.00 + 10.00 x 1 = 10.00 → 61.00 + envío 20.00 = 81.00
        assertThat(guardado.getMontoTotal()).isEqualByComparingTo("81.00");
        assertThat(guardado.getCostoEnvio()).isEqualByComparingTo("20.00");
        assertThat(guardado.getEstado()).isEqualTo(EstadoPedido.PENDIENTE);
        assertThat(guardado.getCliente()).isSameAs(cliente);
        assertThat(guardado.getComercio()).isSameAs(comercio);
        assertThat(guardado.getDetalles()).hasSize(2);

        // El stock se descuenta bajo llave en la misma transacción.
        assertThat(pollo.getStock()).isEqualTo(8);
        assertThat(bebida.getStock()).isEqualTo(4);

        verify(pedidoEstadoService).registrarCreacion(guardado, EMAIL_CLIENTE);
    }

    @Test
    void crear_rechazaStockInsuficienteSinConsumirStockNiPersistir() {
        autenticar(EMAIL_CLIENTE, Rol.CLIENTE);
        when(usuarioRepository.findByEmail(EMAIL_CLIENTE)).thenReturn(Optional.of(cliente));
        when(comercioRepository.findById(10L)).thenReturn(Optional.of(comercio));
        when(productoRepository.findAllByIdInForUpdateOrderByIdAsc(anyList())).thenReturn(List.of(pollo));

        PedidoRequest request = new PedidoRequest(10L, List.of(new ItemPedidoRequest(100L, 99)));

        assertThatThrownBy(() -> pedidoService.crear(request))
                .isInstanceOf(ConflictException.class)
                .satisfies(e -> assertThat(((ConflictException) e).getCodigo()).isEqualTo("STOCK_INSUFICIENTE"));

        assertThat(pollo.getStock()).isEqualTo(10); // sin descuento
        verify(pedidoRepository, never()).save(any());
        verify(pedidoEstadoService, never()).registrarCreacion(any(), anyString());
    }

    @Test
    void crear_rechazaProductoDupLicadoSinConsultarProductos() {
        autenticar(EMAIL_CLIENTE, Rol.CLIENTE);
        when(usuarioRepository.findByEmail(EMAIL_CLIENTE)).thenReturn(Optional.of(cliente));
        when(comercioRepository.findById(10L)).thenReturn(Optional.of(comercio));

        PedidoRequest request = new PedidoRequest(10L, List.of(
                new ItemPedidoRequest(100L, 2),
                new ItemPedidoRequest(100L, 3)));

        assertThatThrownBy(() -> pedidoService.crear(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCodigo()).isEqualTo("PRODUCTOS_DUPLICADOS"));

        verify(productoRepository, never()).findAllByIdInForUpdateOrderByIdAsc(anyList());
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void crear_rechazaProductoDeOtroComercio() {
        autenticar(EMAIL_CLIENTE, Rol.CLIENTE);
        Comercio otroComercio = Comercio.builder().id(99L).nombre("Otro").categoria(CategoriaComercio.SUPERMERCADO)
                .direccion("Z9").abierto(true).activo(true).build();
        Producto deOtro = Producto.builder().id(500L).comercio(otroComercio).nombre("Fideos")
                .precio(new BigDecimal("5.00")).stock(3).disponible(true).build();

        when(usuarioRepository.findByEmail(EMAIL_CLIENTE)).thenReturn(Optional.of(cliente));
        when(comercioRepository.findById(10L)).thenReturn(Optional.of(comercio));
        when(productoRepository.findAllByIdInForUpdateOrderByIdAsc(anyList())).thenReturn(List.of(deOtro));

        PedidoRequest request = new PedidoRequest(10L, List.of(new ItemPedidoRequest(500L, 1)));

        assertThatThrownBy(() -> pedidoService.crear(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCodigo()).isEqualTo("PRODUCTO_OTRO_COMERCIO"));

        verify(pedidoRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // Cancelación con devolución de stock
    // ------------------------------------------------------------------

    @Test
    void cancelar_devuelveElStockExactamenteUnaVez() {
        autenticar(EMAIL_CLIENTE, Rol.CLIENTE);
        Pedido pedido = pedidoBuilder(50L, EstadoPedido.PENDIENTE);
        pollo.setStock(8); // un pedido previo consumió 2

        DetallePedido detalle = DetallePedido.builder().pedido(pedido).producto(pollo).cantidad(2)
                .precioUnitario(new BigDecimal("25.50")).subtotal(new BigDecimal("51.00")).build();

        when(pedidoRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(pedido));
        when(detallePedidoRepository.findByPedidoId(50L)).thenReturn(List.of(detalle));
        when(productoRepository.findAllByIdInForUpdateOrderByIdAsc(List.of(100L))).thenReturn(List.of(pollo));

        // Simula a PedidoEstadoService aplicando la transición real (fija el estado CANCELADO).
        org.mockito.Mockito.doAnswer(invocacion -> {
            pedido.setEstado(EstadoPedido.CANCELADO);
            return null;
        }).when(pedidoEstadoService).aplicar(eq(pedido), eq(EstadoPedido.CANCELADO), anyString());

        pedidoService.cancelar(50L);

        assertThat(pollo.getStock()).isEqualTo(10); // 8 + 2, una única devolución
        verify(pedidoEstadoService).aplicar(pedido, EstadoPedido.CANCELADO, EMAIL_CLIENTE);
        verify(auditoriaService).registrar(eq(EMAIL_CLIENTE), eq("PEDIDO_CANCELAR"), eq("Pedido"),
                eq(50L), anyString());

        // La segunda cancelación encuentra CANCELADO → 409 sin volver a tocar el stock.
        when(pedidoRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(pedido));
        SecurityContextHolder.clearContext();
        autenticar(EMAIL_CLIENTE, Rol.CLIENTE);

        assertThatThrownBy(() -> pedidoService.cancelar(50L))
                .isInstanceOf(ConflictException.class)
                .satisfies(e -> assertThat(((ConflictException) e).getCodigo())
                        .isEqualTo("CANCELACION_NO_PERMITIDA"));
        // El bloqueo de productos se ejecutó una única vez (durante la primera
        // cancelación): la segunda no vuelve a tocar el inventario.
        verify(productoRepository, times(1)).findAllByIdInForUpdateOrderByIdAsc(anyList());
    }

    @Test
    void cancelar_soloPermiteAlDuenoDelPedido() {
        autenticar(EMAIL_CLIENTE, Rol.CLIENTE);
        Usuario otroDueno = Usuario.builder().id(2L).email("otro@entrega.com").rol(Rol.CLIENTE).activo(true).build();
        Pedido pedido = pedidoBuilder(51L, EstadoPedido.PENDIENTE);
        pedido.setCliente(otroDueno);

        when(pedidoRepository.findByIdForUpdate(51L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.cancelar(51L))
                .isInstanceOf(ForbiddenException.class);

        verify(productoRepository, never()).findAllByIdInForUpdateOrderByIdAsc(anyList());
        verify(pedidoEstadoService, never()).aplicar(any(), any(), anyString());
    }

    // ------------------------------------------------------------------
    // Transición administrativa
    // ------------------------------------------------------------------

    @Test
    void cambiarEstadoAdministrativo_soloAdminDePendienteAEnPreparacion() {
        autenticar("admin@entrega.com", Rol.ADMIN);
        Pedido pedido = pedidoBuilder(60L, EstadoPedido.PENDIENTE);
        when(pedidoRepository.findByIdForUpdate(60L)).thenReturn(Optional.of(pedido));

        pedidoService.cambiarEstadoAdministrativo(60L, new EstadoPedidoRequest(EstadoPedido.EN_PREPARACION));

        verify(pedidoEstadoService).aplicar(pedido, EstadoPedido.EN_PREPARACION, "admin@entrega.com");
    }

    @Test
    void cambiarEstadoAdministrativo_rechazaCliente() {
        autenticar(EMAIL_CLIENTE, Rol.CLIENTE);

        assertThatThrownBy(() -> pedidoService.cambiarEstadoAdministrativo(
                60L, new EstadoPedidoRequest(EstadoPedido.EN_PREPARACION)))
                .isInstanceOf(ForbiddenException.class);

        verify(pedidoRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void cambiarEstadoAdministrativo_rechazaOtraTransicion() {
        autenticar("admin@entrega.com", Rol.ADMIN);

        assertThatThrownBy(() -> pedidoService.cambiarEstadoAdministrativo(
                60L, new EstadoPedidoRequest(EstadoPedido.EN_CAMINO)))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCodigo())
                        .isEqualTo("TRANSICION_NO_PERMITIDA"));

        verify(pedidoRepository, never()).findByIdForUpdate(any());
    }

    private Pedido pedidoBuilder(Long id, EstadoPedido estado) {
        return Pedido.builder().id(id).cliente(cliente).comercio(comercio).estado(estado)
                .costoEnvio(new BigDecimal("20.00")).montoTotal(new BigDecimal("81.00")).build();
    }
}