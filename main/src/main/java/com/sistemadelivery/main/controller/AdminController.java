package com.sistemadelivery.main.controller;

import com.sistemadelivery.main.dto.request.UsuarioCrearRequest;
import com.sistemadelivery.main.dto.response.AuditoriaResponse;
import com.sistemadelivery.main.dto.response.EstadisticasResponse;
import com.sistemadelivery.main.dto.response.PageResponse;
import com.sistemadelivery.main.dto.response.UsuarioResponse;
import com.sistemadelivery.main.entity.enums.Rol;
import com.sistemadelivery.main.service.AuditoriaService;
import com.sistemadelivery.main.service.EstadisticaService;
import com.sistemadelivery.main.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * Endpoints administrativos bajo /api/v1/admin.
 * Todo el paquete está protegido con hasRole('ADMIN') en SecurityConfig
 * y reforzado con @PreAuthorize en cada método.
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administración", description = "Auditoría, estadísticas y usuarios (solo ADMIN)")
public class AdminController {

    private final AuditoriaService auditoriaService;
    private final EstadisticaService estadisticaService;
    private final UsuarioService usuarioService;

    @Operation(summary = "Consulta de auditoría con filtros (solo ADMIN)")
    @GetMapping("/auditoria")
    public ResponseEntity<PageResponse<AuditoriaResponse>> auditoria(
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) String accion,
            @RequestParam(required = false) String entidad,
            @RequestParam(required = false) Long entidadId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(auditoriaService.buscar(usuario, accion, entidad, entidadId, desde, hasta, page, size));
    }

    @Operation(summary = "Estadísticas básicas de pedidos y ventas (solo ADMIN)")
    @GetMapping("/estadisticas")
    public ResponseEntity<EstadisticasResponse> estadisticas() {
        return ResponseEntity.ok(estadisticaService.obtener());
    }

    @Operation(summary = "Listar usuarios, opcionalmente por rol (solo ADMIN)")
    @GetMapping("/usuarios")
    public ResponseEntity<PageResponse<UsuarioResponse>> usuarios(
            @RequestParam(required = false) Rol rol,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(usuarioService.listar(rol, page, size));
    }

    @Operation(summary = "Listar repartidores (solo ADMIN)")
    @GetMapping("/repartidores")
    public ResponseEntity<PageResponse<UsuarioResponse>> repartidores(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(usuarioService.listar(Rol.REPARTIDOR, page, size));
    }

    @Operation(summary = "Crear usuario con rol CLIENTE o REPARTIDOR (solo ADMIN)",
            description = "La creación de administradores no está disponible por API: "
                    + "se realiza con ADMIN_EMAIL/ADMIN_PASSWORD al iniciar la aplicación.")
    @PostMapping("/usuarios")
    public ResponseEntity<UsuarioResponse> crearUsuario(@Valid @RequestBody UsuarioCrearRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.crear(request));
    }
}
