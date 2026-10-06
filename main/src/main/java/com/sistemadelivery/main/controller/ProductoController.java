package com.sistemadelivery.main.controller;

import com.sistemadelivery.main.dto.request.DisponibilidadRequest;
import com.sistemadelivery.main.dto.request.ProductoRequest;
import com.sistemadelivery.main.dto.request.StockRequest;
import com.sistemadelivery.main.dto.response.PageResponse;
import com.sistemadelivery.main.dto.response.ProductoResponse;
import com.sistemadelivery.main.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
@Tag(name = "Productos", description = "Catálogo público y gestión de inventario")
public class ProductoController {

    private final ProductoService productoService;

    @Operation(summary = "Listar productos de un comercio (público)",
            description = "Con gestion=true (solo ADMIN) devuelve también los no disponibles y sin stock.")
    @GetMapping("/comercios/{comercioId}/productos")
    public ResponseEntity<PageResponse<ProductoResponse>> listarPorComercio(
            @PathVariable Long comercioId,
            @RequestParam(defaultValue = "false") boolean gestion,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(productoService.listarPorComercio(comercioId, gestion, page, size));
    }

    @Operation(summary = "Consultar un producto (público)")
    @GetMapping("/productos/{id}")
    public ResponseEntity<ProductoResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(productoService.obtener(id));
    }

    @Operation(summary = "Crear producto en un comercio (solo ADMIN)")
    @PostMapping("/comercios/{comercioId}/productos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductoResponse> crear(@PathVariable Long comercioId,
                                                  @Valid @RequestBody ProductoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productoService.crear(comercioId, request));
    }

    @Operation(summary = "Actualizar producto: nombre, descripción, precio y categoría (solo ADMIN)",
            description = "Un cambio de precio afecta solo a pedidos nuevos: los históricos conservan su precio unitario.")
    @PutMapping("/productos/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductoResponse> actualizar(@PathVariable Long id,
                                                       @Valid @RequestBody ProductoRequest request) {
        return ResponseEntity.ok(productoService.actualizar(id, request));
    }

    @Operation(summary = "Ajustar stock de forma absoluta (solo ADMIN)")
    @PatchMapping("/productos/{id}/stock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductoResponse> ajustarStock(@PathVariable Long id,
                                                         @Valid @RequestBody StockRequest request) {
        return ResponseEntity.ok(productoService.ajustarStock(id, request));
    }

    @Operation(summary = "Activar/desactivar disponibilidad (solo ADMIN)")
    @PatchMapping("/productos/{id}/disponibilidad")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> cambiarDisponibilidad(@PathVariable Long id,
                                                      @Valid @RequestBody DisponibilidadRequest request) {
        productoService.cambiarDisponibilidad(id, request);
        return ResponseEntity.noContent().build();
    }
}
