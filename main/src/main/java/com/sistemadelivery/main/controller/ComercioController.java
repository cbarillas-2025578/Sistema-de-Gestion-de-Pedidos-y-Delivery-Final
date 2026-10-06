package com.sistemadelivery.main.controller;

import com.sistemadelivery.main.dto.request.ComercioRequest;
import com.sistemadelivery.main.dto.request.EstadoComercioRequest;
import com.sistemadelivery.main.dto.response.ComercioResponse;
import com.sistemadelivery.main.dto.response.PageResponse;
import com.sistemadelivery.main.entity.enums.CategoriaComercio;
import com.sistemadelivery.main.service.ComercioService;
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
@RequestMapping("/api/v1/comercios")
@RequiredArgsConstructor
@Validated
@Tag(name = "Comercios", description = "Catálogo público y gestión administrativa")
public class ComercioController {

    private final ComercioService comercioService;

    @Operation(summary = "Listar comercios activos y abiertos (público)")
    @GetMapping
    public ResponseEntity<PageResponse<ComercioResponse>> listar(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(comercioService.listarAbiertos(page, size));
    }

    @Operation(summary = "Listado de gestión con filtros (solo ADMIN)")
    @GetMapping("/gestion")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<ComercioResponse>> gestion(
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) Boolean abierto,
            @RequestParam(required = false) CategoriaComercio categoria,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(comercioService.listarParaGestion(activo, abierto, categoria, q, page, size));
    }

    @Operation(summary = "Consultar un comercio (público)")
    @GetMapping("/{id}")
    public ResponseEntity<ComercioResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(comercioService.obtener(id));
    }

    @Operation(summary = "Crear comercio (solo ADMIN)")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ComercioResponse> crear(@Valid @RequestBody ComercioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(comercioService.crear(request));
    }

    @Operation(summary = "Actualizar comercio (solo ADMIN)")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ComercioResponse> actualizar(@PathVariable Long id,
                                                       @Valid @RequestBody ComercioRequest request) {
        return ResponseEntity.ok(comercioService.actualizar(id, request));
    }

    @Operation(summary = "Modificar estado operativo activo/abierto (solo ADMIN)")
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ComercioResponse> cambiarEstado(@PathVariable Long id,
                                                          @RequestBody EstadoComercioRequest request) {
        return ResponseEntity.ok(comercioService.cambiarEstado(id, request));
    }
}
