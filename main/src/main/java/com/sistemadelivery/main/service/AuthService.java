package com.sistemadelivery.main.service;

import com.sistemadelivery.main.dto.request.LoginRequest;
import com.sistemadelivery.main.dto.request.RegistroRequest;
import com.sistemadelivery.main.dto.response.AuthResponse;

public interface AuthService {

    /** Registra un cliente (rol fijo CLIENTE) y emite su token. */
    AuthResponse registrar(RegistroRequest request);

    /** Autentica email/contraseña y emite el token JWT. */
    AuthResponse login(LoginRequest request);
}
