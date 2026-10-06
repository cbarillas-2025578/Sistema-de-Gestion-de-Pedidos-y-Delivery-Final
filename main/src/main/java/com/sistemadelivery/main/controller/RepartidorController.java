package com.sistemadelivery.main.controller;

import com.sistemadelivery.main.dto.request.EstadoPedidoRequest;
import com.sistemadelivery.main.dto.response.PageResponse;
import com.sistemadelivery.main.dto.response.PedidoResponse;
import com.sistemadelivery.main.service.RepartidorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/repartidores")
@RequiredArgsConstructor
@Validated
@Tag(name = "Repartidores", description = "Consulta, aceptación y actualización de entregas")
public class RepartidorController {

    private final RepartidorService repartidorService;

    @Operation(summary = "Pedidos disponibles para entrega (repartidor autenticado)",
            description = "Pedidos en estado EN_PREPARACION sin repartidor asignado.")
    @GetMapping("/pedidos-disponibles")
    public ResponseEntity<PageResponse<PedidoResponse>> pedidosDisponibles(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(repartidorService.pedidosDisponibles(page, size));
    }

    @Operation(summary = "Pedidos asignados al repartidor autenticado")
    @GetMapping("/mis-pedidos")
    public ResponseEntity<PageResponse<PedidoResponse>> misPedidos(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(repartidorService.misPedidos(page, size));
    }

    @Operation(summary = "Aceptar un pedido disponible",
            description = "Asignación atómica: si otro repartidor lo aceptó primero responde 409.")
    @PostMapping("/pedidos/{id}/aceptar")
    public ResponseEntity<PedidoResponse> aceptar(@PathVariable Long id) {
        return ResponseEntity.ok(repartidorService.aceptar(id));
    }

    @Operation(summary = "Actualizar estado de entrega",
            description = "Transiciones permitidas para el repartidor asignado: "
                    + "EN_PREPARACION → EN_CAMINO → ENTREGADO.")
    @PatchMapping("/pedidos/{id}/estado")
    public ResponseEntity<PedidoResponse> actualizarEstado(@PathVariable Long id,
                                                           @Valid @RequestBody EstadoPedidoRequest request) {
        return ResponseEntity.ok(repartidorService.actualizarEstado(id, request));
    }
}
