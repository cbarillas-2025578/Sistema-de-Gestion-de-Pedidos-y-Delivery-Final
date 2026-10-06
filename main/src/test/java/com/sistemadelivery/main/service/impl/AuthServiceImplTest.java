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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de AuthServiceImpl:
 * - el registro público SIEMPRE crea rol CLIENTE (el rol nunca viene del cliente);
 * - email duplicado → 409;
 * - login con credenciales inválidas → 401 genérico (no revela si el correo existe).
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuditoriaService auditoriaService;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(usuarioRepository, passwordEncoder, authenticationManager,
                jwtService, auditoriaService);
    }

    @Test
    void registrar_rechazaEmailDuplicado() {
        when(usuarioRepository.existsByEmail("ana@correo.com")).thenReturn(true);

        RegistroRequest request = new RegistroRequest("Ana", "  ANA@Correo.com ", "Clave#123", null, "12345678");

        assertThatThrownBy(() -> authService.registrar(request))
                .isInstanceOf(ConflictException.class)
                .satisfies(e -> assertThat(((ConflictException) e).getCodigo()).isEqualTo("EMAIL_DUPLICADO"));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void registrar_creaSoloClientesYNoExponeElRolSolicitado() {
        when(usuarioRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("Clave#123")).thenReturn("$2a$10$hashfake");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
        when(jwtService.generarToken(any(Usuario.class))).thenReturn("token.jwt.prueba");
        when(jwtService.getExpiracionMs()).thenReturn(3600000L);

        RegistroRequest request = new RegistroRequest("Ana", " Ana@Correo.com ", "Clave#123", "Zona 10", "12345678");
        AuthResponse respuesta = authService.registrar(request);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario guardado = captor.getValue();

        // El rol es fijo, nunca tomado de la solicitud.
        assertThat(guardado.getRol()).isEqualTo(Rol.CLIENTE);
        assertThat(guardado.getEmail()).isEqualTo("ana@correo.com"); // normalizado
        assertThat(guardado.getActivo()).isTrue();
        assertThat(guardado.getPassword()).isEqualTo("$2a$10$hashfake"); // BCrypt

        assertThat(respuesta.token()).isEqualTo("token.jwt.prueba");
        assertThat(respuesta.rol()).isEqualTo(Rol.CLIENTE);
        assertThat(respuesta.tipo()).isEqualTo("Bearer");

        verify(auditoriaService).registrar(eq("ana@correo.com"), eq("USUARIO_REGISTRO"),
                eq("Usuario"), isNull(), anyString());
    }

    @Test
    void login_devuelveTokenCuandoLasCredencialesSonValidas() {
        Usuario usuario = Usuario.builder().id(7L).nombre("Ana").email("cliente@correo.com")
                .rol(Rol.CLIENTE).activo(true).build();
        when(usuarioRepository.findByEmail("cliente@correo.com")).thenReturn(Optional.of(usuario));
        when(jwtService.generarToken(usuario)).thenReturn("token.jwt.valido");
        when(jwtService.getExpiracionMs()).thenReturn(3600000L);

        AuthResponse respuesta = authService.login(new LoginRequest("cliente@correo.com", "Clave#123"));

        assertThat(respuesta.token()).isEqualTo("token.jwt.valido");
        assertThat(respuesta.email()).isEqualTo("cliente@correo.com");
        verify(authenticationManager).authenticate(any());
        verify(auditoriaService).registrar(eq("cliente@correo.com"), eq("LOGIN"),
                eq("Sesion"), eq(7L), anyString());
    }

    @Test
    void login_rechazaCredencialesInvalidas() {
        doThrow(new BadCredentialsException("credenciales inválidas"))
                .when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(new LoginRequest("cliente@correo.com", "mal")))
                .isInstanceOf(UnauthorizedException.class)
                .satisfies(e -> assertThat(((UnauthorizedException) e).getCodigo())
                        .isEqualTo("CREDENCIALES_INVALIDAS"));

        verify(usuarioRepository, never()).findByEmail(anyString());
    }

    @Test
    void login_rechazaUsuarioDesactivado() {
        doThrow(new DisabledException("usuario desactivado"))
                .when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(new LoginRequest("cliente@correo.com", "Clave#123")))
                .isInstanceOf(UnauthorizedException.class)
                .satisfies(e -> assertThat(((UnauthorizedException) e).getCodigo())
                        .isEqualTo("USUARIO_DESACTIVADO"));
    }
}