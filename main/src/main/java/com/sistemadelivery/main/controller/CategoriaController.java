package com.sistemadelivery.main.controller;

import com.sistemadelivery.main.dto.request.CategoriaRequest;
import com.sistemadelivery.main.dto.request.EstadoRequest;
import com.sistemadelivery.main.dto.response.CategoriaResponse;
import com.sistemadelivery.main.service.CategoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categorias")
@RequiredArgsConstructor
@Tag(name = "Categorías globales", description = "Categorías globales de productos (no confundir con la categoría del comercio)")
public class CategoriaController {

    private final CategoriaService categoriaService;

    @Operation(summary = "Listar categorías activas (público)")
    @GetMapping
    public ResponseEntity<List<CategoriaResponse>> listarActivas() {
        return ResponseEntity.ok(categoriaService.listarActivas());
    }

    @Operation(summary = "Crear categoría (solo ADMIN)")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoriaResponse> crear(@Valid @RequestBody CategoriaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoriaService.crear(request));
    }

    @Operation(summary = "Actualizar categoría (solo ADMIN)")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoriaResponse> actualizar(@PathVariable Long id,
                                                        @Valid @RequestBody CategoriaRequest request) {
        return ResponseEntity.ok(categoriaService.actualizar(id, request));
    }

    @Operation(summary = "Activar/desactivar categoría (solo ADMIN)")
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoriaResponse> cambiarEstado(@PathVariable Long id,
                                                           @Valid @RequestBody EstadoRequest request) {
        return ResponseEntity.ok(categoriaService.cambiarEstado(id, request.activo()));
    }
}
