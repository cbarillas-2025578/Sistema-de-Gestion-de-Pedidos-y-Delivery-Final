package com.sistemadelivery.main.service.impl;

import com.sistemadelivery.main.dto.request.LoginRequest;
import com.sistemadelivery.main.dto.request.RegistroRequest;
import com.sistemadelivery.main.dto.response.AuthResponse;
import com.sistemadelivery.main.entity.Usuario;
import com.sistemadelivery.main.entity.enums.Rol;
import com.sistemadelivery.main.exception.ConflictException;
import com.sistemadelivery.main.exception.UnauthorizedException;
import com.sistemadelivery.main.repository.UsuarioRepository;
import com.sistemadelivery.main.security.jwt.JwtService;
import com.sistemadelivery.main.service.AuditoriaService;
import com.sistemadelivery.main.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuditoriaService auditoriaService;

    @Override
    @Transactional
    public AuthResponse registrar(RegistroRequest request) {
        String email = normalizar(request.email());

        if (usuarioRepository.existsByEmail(email)) {
            throw new ConflictException("EMAIL_DUPLICADO", "El correo electrónico ya está registrado");
        }

        // El registro público SOLO crea clientes: el rol nunca proviene del cliente.
        Usuario usuario = Usuario.builder()
                .nombre(request.nombre().trim())
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .rol(Rol.CLIENTE)
                .direccion(request.direccion())
                .telefono(request.telefono())
                .activo(true)
                .build();

        usuario = usuarioRepository.save(usuario);

        auditoriaService.registrar(email, "USUARIO_REGISTRO", "Usuario", usuario.getId(),
                "Cliente registrado");

        return emitirToken(usuario);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String email = normalizar(request.email());

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password()));
        } catch (DisabledException e) {
            throw new UnauthorizedException("USUARIO_DESACTIVADO", "La cuenta está desactivada");
        } catch (BadCredentialsException e) {
            // Mensaje genérico: no revela si el correo existe.
            throw new UnauthorizedException("CREDENCIALES_INVALIDAS", "Correo o contraseña incorrectos");
        } catch (AuthenticationException e) {
            throw new UnauthorizedException("AUTENTICACION_FALLIDA", "No se pudo autenticar al usuario");
        }

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException(
                        "CREDENCIALES_INVALIDAS", "Correo o contraseña incorrectos"));

        auditoriaService.registrar(email, "LOGIN", "Sesion", usuario.getId(), "Inicio de sesión exitoso");

        return emitirToken(usuario);
    }

    private AuthResponse emitirToken(Usuario usuario) {
        String token = jwtService.generarToken(usuario);
        return AuthResponse.de(token, jwtService.getExpiracionMs(),
                usuario.getId(), usuario.getNombre(), usuario.getEmail(), usuario.getRol());
    }

    private String normalizar(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
