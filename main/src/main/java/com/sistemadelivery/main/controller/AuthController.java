package com.sistemadelivery.main.controller;

import com.sistemadelivery.main.dto.request.LoginRequest;
import com.sistemadelivery.main.dto.request.RegistroRequest;
import com.sistemadelivery.main.dto.response.AuthResponse;
import com.sistemadelivery.main.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Registro de clientes e inicio de sesión")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Registrar un cliente",
            description = "El registro público siempre crea usuarios con rol CLIENTE.")
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> registrar(@Valid @RequestBody RegistroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(request));
    }

    @Operation(summary = "Iniciar sesión", description = "Devuelve un token JWT Bearer válido por JWT_EXPIRATION_MS.")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
