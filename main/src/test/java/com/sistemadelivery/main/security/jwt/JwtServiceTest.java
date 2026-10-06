package com.sistemadelivery.main.security.jwt;

import com.sistemadelivery.main.entity.Usuario;
import com.sistemadelivery.main.entity.enums.Rol;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas unitarias de JwtService.
 * Verifica que la clave se valida al iniciar (>= 32 caracteres) y el round-trip
 * firma/verificación, incluido el rechazo de firmas ajenas.
 */
class JwtServiceTest {

    private static final String SECRETO_VALIDO = "clave-secreta-de-prueba-con-mas-de-32-caracteres";

    @Test
    void generaYExtraeElEmailDelToken() {
        JwtService jwtService = new JwtService(SECRETO_VALIDO, 3600000L);
        jwtService.validarConfiguracion();

        Usuario usuario = Usuario.builder().id(42L).email("cliente@entrega.com").rol(Rol.CLIENTE)
                .nombre("Cliente").build();

        String token = jwtService.generarToken(usuario);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extraerEmail(token)).isEqualTo("cliente@entrega.com");
        assertThat(jwtService.getExpiracionMs()).isEqualTo(3600000L);
    }

    @Test
    void rechazaTokenFirmadoConOtraClave() {
        JwtService emisor = new JwtService(SECRETO_VALIDO, 3600000L);
        emisor.validarConfiguracion();
        String token = emisor.generarToken(
                Usuario.builder().id(1L).email("a@b.com").rol(Rol.CLIENTE).build());

        JwtService receptor = new JwtService("otra-clave-secreta-distinta-con-mas-de-32-caracteres", 3600000L);
        receptor.validarConfiguracion();

        assertThatThrownBy(() -> receptor.extraerEmail(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void rechazaSecretoDemasiadoCortoAlInicializar() {
        JwtService invalido = new JwtService("muy-corto", 3600000L);
        assertThatThrownBy(invalido::validarConfiguracion)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void rechazaExpiracionNoPositivaAlInicializar() {
        JwtService invalido = new JwtService(SECRETO_VALIDO, 0);
        assertThatThrownBy(invalido::validarConfiguracion)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_EXPIRATION_MS");
    }
}