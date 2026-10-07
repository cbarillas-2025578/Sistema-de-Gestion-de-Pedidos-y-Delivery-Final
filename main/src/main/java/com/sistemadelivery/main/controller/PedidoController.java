package com.sistemadelivery.main.controller;

import com.sistemadelivery.main.dto.request.EstadoPedidoRequest;
import com.sistemadelivery.main.dto.request.PedidoRequest;
import com.sistemadelivery.main.dto.response.HistorialEstadoResponse;
import com.sistemadelivery.main.dto.response.PageResponse;
import com.sistemadelivery.main.dto.response.PedidoResponse;
import com.sistemadelivery.main.entity.enums.EstadoPedido;
import com.sistemadelivery.main.notification.NotificacionService;
import com.sistemadelivery.main.service.PedidoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/pedidos")
@RequiredArgsConstructor
@Validated
@Tag(name = "Pedidos", description = "Creación, consulta, cancelación y seguimiento de pedidos")
public class PedidoController {

    private final PedidoService pedidoService;
    private final NotificacionService notificacionService;

    @Operation(summary = "Crear pedido multiproducto (solo CLIENTE)",
            description = "El cliente envía pares productoId/cantidad en 'productos' (también se acepta 'items') "
                    + "y, opcionalmente, comercioId: si se omite, el comercio se deduce de los productos. "
                    + "Los precios, subtotales, costo de envío (Q20.00) y el total "
                    + "se calculan exclusivamente en el servidor.")
    @PostMapping
    public ResponseEntity<PedidoResponse> crear(@Valid @RequestBody PedidoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pedidoService.crear(request));
    }

    @Operation(summary = "Pedidos del cliente autenticado")
    @GetMapping("/mis-pedidos")
    public ResponseEntity<PageResponse<PedidoResponse>> misPedidos(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(pedidoService.misPedidos(page, size));
    }

    @Operation(summary = "Listado administrativo con filtros (solo ADMIN)")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<PedidoResponse>> listarTodos(
            @RequestParam(required = false) EstadoPedido estado,
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) Long repartidorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(pedidoService.listarTodos(estado, clienteId, repartidorId, desde, hasta, page, size));
    }

    @Operation(summary = "Detalle de un pedido",
            description = "El cliente solo accede a los suyos; el repartidor a los asignados; el administrador a todos.")
    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.obtener(id));
    }

    @Operation(summary = "Cancelar un pedido propio en estado PENDIENTE (CLIENTE o ADMIN)",
            description = "Devuelve el stock al inventario exactamente una vez. El cliente solo puede cancelar sus propios pedidos.")
    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('CLIENTE', 'ADMIN')")
    public ResponseEntity<PedidoResponse> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.cancelar(id));
    }

    @Operation(summary = "Transición de estado del pedido (ADMIN o REPARTIDOR)",
            description = "Actualiza el estado del pedido respetando las transiciones autorizadas. "
                    + "Roles permitidos: ADMIN, REPARTIDOR.")
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMIN', 'REPARTIDOR')")
    public ResponseEntity<PedidoResponse> cambiarEstado(@PathVariable Long id,
                                                        @Valid @RequestBody EstadoPedidoRequest request) {
        return ResponseEntity.ok(pedidoService.cambiarEstadoAdministrativo(id, request));
    }

    @Operation(summary = "Pedidos pendientes de asignación o disponibles para entrega",
            description = "Muestra los pedidos en estado PENDIENTE o EN_PREPARACION sin repartidor asignado. "
                    + "Roles permitidos: ADMIN, REPARTIDOR.")
    @GetMapping("/disponibles")
    @PreAuthorize("hasAnyRole('ADMIN', 'REPARTIDOR')")
    public ResponseEntity<PageResponse<PedidoResponse>> pedidosDisponibles(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(pedidoService.pedidosDisponibles(page, size));
    }

    @Operation(summary = "Historial de cambios de estado de un pedido")
    @GetMapping("/{id}/historial")
    public ResponseEntity<List<HistorialEstadoResponse>> historial(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.historial(id));
    }

    @Operation(summary = "Seguimiento en tiempo real (Server-Sent Events)",
            description = "Suscripción SSE a los cambios de estado. Verifica que el cliente solo siga sus propios "
                    + "pedidos (o los asignados, en el caso del repartidor). Eventos: 'conexion' y 'estado'.")
    @GetMapping(value = "/{id}/seguimiento", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter seguir(@PathVariable Long id) {
        // obtener(id) aplica la verificación de acceso al recurso antes de suscribir.
        pedidoService.obtener(id);
        return notificacionService.suscribir(id);
    }
}
