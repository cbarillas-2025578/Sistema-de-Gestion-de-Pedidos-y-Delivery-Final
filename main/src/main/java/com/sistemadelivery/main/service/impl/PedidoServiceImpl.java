package com.sistemadelivery.main.service.impl;

import com.sistemadelivery.main.dto.request.EstadoPedidoRequest;
import com.sistemadelivery.main.dto.request.ItemPedidoRequest;
import com.sistemadelivery.main.dto.request.PedidoRequest;
import com.sistemadelivery.main.dto.response.HistorialEstadoResponse;
import com.sistemadelivery.main.dto.response.PageResponse;
import com.sistemadelivery.main.dto.response.PedidoResponse;
import com.sistemadelivery.main.entity.Comercio;
import com.sistemadelivery.main.entity.DetallePedido;
import com.sistemadelivery.main.entity.Pedido;
import com.sistemadelivery.main.entity.Producto;
import com.sistemadelivery.main.entity.Usuario;
import com.sistemadelivery.main.entity.enums.EstadoPedido;
import com.sistemadelivery.main.exception.BusinessException;
import com.sistemadelivery.main.exception.ConflictException;
import com.sistemadelivery.main.exception.ForbiddenException;
import com.sistemadelivery.main.exception.ResourceNotFoundException;
import com.sistemadelivery.main.exception.UnauthorizedException;
import com.sistemadelivery.main.mapper.PedidoMapper;
import com.sistemadelivery.main.repository.ComercioRepository;
import com.sistemadelivery.main.repository.DetallePedidoRepository;
import com.sistemadelivery.main.repository.HistorialEstadoPedidoRepository;
import com.sistemadelivery.main.repository.PedidoRepository;
import com.sistemadelivery.main.repository.ProductoRepository;
import com.sistemadelivery.main.repository.UsuarioRepository;
import com.sistemadelivery.main.service.AuditoriaService;
import com.sistemadelivery.main.service.PedidoEstadoService;
import com.sistemadelivery.main.service.PedidoService;
import com.sistemadelivery.main.util.SeguridadUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PedidoServiceImpl implements PedidoService {

    /** Costo fijo de envío por pedido (§6.1). */
    public static final BigDecimal COSTO_ENVIO = new BigDecimal("20.00");
    private static final int ESCALA_MONETARIA = 2;

    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;
    private final ComercioRepository comercioRepository;
    private final UsuarioRepository usuarioRepository;
    private final DetallePedidoRepository detallePedidoRepository;
    private final HistorialEstadoPedidoRepository historialRepository;
    private final PedidoMapper mapper;
    private final PedidoEstadoService pedidoEstadoService;
    private final AuditoriaService auditoriaService;

    @Override
    @Transactional
    public PedidoResponse crear(PedidoRequest request) {
        String emailCliente = SeguridadUtil.emailActual();
        Usuario cliente = usuarioRepository.findByEmail(emailCliente)
                .orElseThrow(() -> new UnauthorizedException("USUARIO_NO_ENCONTRADO",
                        "El usuario autenticado no existe"));
        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw new ForbiddenException("El usuario está desactivado");
        }

        Comercio comercio = comercioRepository.findById(request.comercioId())
                .orElseThrow(() -> ResourceNotFoundException.de("Comercio", request.comercioId()));
        if (!Boolean.TRUE.equals(comercio.getActivo())) {
            throw new ConflictException("COMERCIO_INACTIVO",
                    "El comercio está inactivo y no puede recibir pedidos");
        }
        if (!Boolean.TRUE.equals(comercio.getAbierto())) {
            throw new ConflictException("COMERCIO_CERRADO",
                    "El comercio está cerrado y no puede recibir pedidos");
        }

        Map<Long, Integer> cantidades = consolidar(request.productos());
        List<Long> ids = cantidades.keySet().stream().sorted().toList(); // orden estable

        // Bloqueo pesado de TODOS los productos en orden id ASC:
        // garantiza consistencia ante concurrencia y evita interbloqueos.
        List<Producto> productos = productoRepository.findAllByIdInForUpdateOrderByIdAsc(ids);
        if (productos.size() != ids.size()) {
            Set<Long> encontrados = productos.stream().map(Producto::getId).collect(Collectors.toSet());
            Long faltante = ids.stream().filter(id -> !encontrados.contains(id)).findFirst().orElseThrow();
            throw ResourceNotFoundException.de("Producto", faltante);
        }

        BigDecimal totalProductos = BigDecimal.ZERO;
        List<DetallePedido> detalles = new ArrayList<>();

        for (Producto producto : productos) {
            if (!producto.getComercio().getId().equals(comercio.getId())) {
                throw new BusinessException("PRODUCTO_OTRO_COMERCIO",
                        "El producto '" + producto.getNombre() + "' no pertenece al comercio seleccionado");
            }
            if (!Boolean.TRUE.equals(producto.getDisponible())) {
                throw new ConflictException("PRODUCTO_NO_DISPONIBLE",
                        "El producto '" + producto.getNombre() + "' no está disponible");
            }
            int cantidad = cantidades.get(producto.getId());
            if (producto.getStock() < cantidad) {
                throw new ConflictException("STOCK_INSUFICIENTE",
                        "Stock insuficiente para '" + producto.getNombre()
                                + "' (disponible: " + producto.getStock() + ", solicitado: " + cantidad + ")");
            }

            BigDecimal subtotal = producto.getPrecio()
                    .multiply(BigDecimal.valueOf(cantidad))
                    .setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);
            totalProductos = totalProductos.add(subtotal);

            detalles.add(DetallePedido.builder()
                    .producto(producto)
                    .cantidad(cantidad)
                    .precioUnitario(producto.getPrecio())
                    .subtotal(subtotal)
                    .build());

            producto.setStock(producto.getStock() - cantidad); // bajo bloqueo
        }

        BigDecimal montoTotal = totalProductos.add(COSTO_ENVIO).setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);

        Pedido pedido = Pedido.builder()
                .cliente(cliente)
                .comercio(comercio)
                .costoEnvio(COSTO_ENVIO)
                .montoTotal(montoTotal)
                .estado(EstadoPedido.PENDIENTE)
                .build();
        detalles.forEach(pedido::addDetalle);

        Pedido guardado = pedidoRepository.save(pedido);
        pedidoEstadoService.registrarCreacion(guardado, emailCliente);
        return mapper.aRespuesta(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PedidoResponse> misPedidos(int page, int size) {
        Usuario cliente = usuarioAutenticado();
        Page<Pedido> pedidos = pedidoRepository.findByClienteId(cliente.getId(),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fechaPedido")));
        return PageResponse.de(pedidos.map(mapper::aRespuesta));
    }

    @Override
    @Transactional(readOnly = true)
    public PedidoResponse obtener(Long id) {
        Pedido pedido = cargarYVerificarAcceso(id);
        return mapper.aRespuesta(pedido);
    }

    @Override
    @Transactional
    public PedidoResponse cancelar(Long id) {
        String email = SeguridadUtil.emailActual();

        // Bloqueo de la fila del pedido: dos cancelaciones simultáneas se serializan
        // y la segunda observa CANCELADO → 409 (el stock solo se devuelve una vez).
        Pedido pedido = pedidoRepository.findByIdForUpdate(id)
                .orElseThrow(() -> ResourceNotFoundException.de("Pedido", id));
        if (!pedido.getCliente().getEmail().equals(email)) {
            throw new ForbiddenException("Solo puede cancelar sus propios pedidos");
        }
        if (pedido.getEstado() != EstadoPedido.PENDIENTE) {
            throw new ConflictException("CANCELACION_NO_PERMITIDA",
                    "Solo se pueden cancelar pedidos en estado PENDIENTE; estado actual: "
                            + pedido.getEstado());
        }

        List<DetallePedido> detalles = detallePedidoRepository.findByPedidoId(id);
        Map<Long, Integer> aDevolver = detalles.stream()
                .collect(Collectors.toMap(d -> d.getProducto().getId(), DetallePedido::getCantidad));

        if (!aDevolver.isEmpty()) {
            List<Long> ids = aDevolver.keySet().stream().sorted().toList(); // mismo orden que al crear
            List<Producto> productos = productoRepository.findAllByIdInForUpdateOrderByIdAsc(ids);
            for (Producto producto : productos) {
                producto.setStock(producto.getStock() + aDevolver.get(producto.getId()));
            }
        }

        // La máquina de estados exige PENDIENTE → CANCELADO; aplicar() fija el
        // estado, registra historial/auditoría y publica la notificación.
        pedidoEstadoService.aplicar(pedido, EstadoPedido.CANCELADO, email);
        auditoriaService.registrar(email, "PEDIDO_CANCELAR", "Pedido", id,
                "Cancelado por el cliente; stock devuelto: " + aDevolver);
        return mapper.aRespuesta(pedido);
    }

    @Override
    @Transactional
    public PedidoResponse cambiarEstadoAdministrativo(Long id, EstadoPedidoRequest request) {
        String email = SeguridadUtil.emailActual();
        if (!SeguridadUtil.esAdmin()) {
            throw new ForbiddenException("Solo un administrador puede ejecutar esta operación");
        }
        if (request.estado() != EstadoPedido.EN_PREPARACION) {
            throw new BusinessException("TRANSICION_NO_PERMITIDA",
                    "La única transición administrativa admitida es PENDIENTE → EN_PREPARACION");
        }

        Pedido pedido = pedidoRepository.findByIdForUpdate(id)
                .orElseThrow(() -> ResourceNotFoundException.de("Pedido", id));
        if (pedido.getEstado() != EstadoPedido.PENDIENTE) {
            throw new ConflictException("TRANSICION_INVALIDA",
                    "Transición inválida: " + pedido.getEstado() + " → EN_PREPARACION");
        }

        pedidoEstadoService.aplicar(pedido, EstadoPedido.EN_PREPARACION, email);
        return mapper.aRespuesta(pedido);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PedidoResponse> listarTodos(EstadoPedido estado, Long clienteId, Long repartidorId,
                                                    LocalDateTime desde, LocalDateTime hasta,
                                                    int page, int size) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new BusinessException("RANGO_FECHAS_INVALIDO",
                    "La fecha 'desde' no puede ser posterior a la fecha 'hasta'");
        }
        Page<Pedido> pedidos = pedidoRepository.buscar(estado, clienteId, repartidorId, desde, hasta,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fechaPedido")));
        return PageResponse.de(pedidos.map(mapper::aRespuesta));
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistorialEstadoResponse> historial(Long id) {
        cargarYVerificarAcceso(id);
        return historialRepository.findByPedidoIdOrderByFechaAscIdAsc(id).stream()
                .map(mapper::aRespuesta)
                .toList();
    }

    /** Regla de acceso a nivel de recurso: dueño, repartidor asignado o administrador. */
    private Pedido cargarYVerificarAcceso(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.de("Pedido", id));
        if (SeguridadUtil.esAdmin()) {
            return pedido;
        }
        String email = SeguridadUtil.emailActual();
        boolean esDueno = pedido.getCliente().getEmail().equals(email);
        boolean esRepartidorAsignado = pedido.getRepartidor() != null
                && pedido.getRepartidor().getEmail().equals(email);
        if (!esDueno && !esRepartidorAsignado) {
            throw new ForbiddenException("No tiene acceso a este pedido");
        }
        return pedido;
    }

    /**
     * Valida los ítems del cliente: cantidades positivas y sin duplicados.
     * Devuelve productoId → cantidad en un mapa (consolidado defensivo).
     */
    private Map<Long, Integer> consolidar(List<ItemPedidoRequest> items) {
        Map<Long, Integer> cantidades = new HashMap<>();
        for (ItemPedidoRequest item : items) {
            if (item.productoId() == null || item.cantidad() == null || item.cantidad() <= 0) {
                throw new BusinessException("ITEM_INVALIDO",
                        "Cada producto debe tener id y cantidad positivos");
            }
            Integer anterior = cantidades.put(item.productoId(), item.cantidad());
            if (anterior != null) {
                throw new BusinessException("PRODUCTOS_DUPLICADOS",
                        "El producto " + item.productoId() + " aparece más de una vez; "
                                + "consolide las cantidades en una sola línea");
            }
        }
        if (cantidades.isEmpty()) {
            throw new BusinessException("PEDIDO_VACIO", "El pedido debe incluir al menos un producto");
        }
        return cantidades;
    }

    private Usuario usuarioAutenticado() {
        String email = SeguridadUtil.emailActual();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("USUARIO_NO_ENCONTRADO",
                        "El usuario autenticado no existe"));
    }
}
