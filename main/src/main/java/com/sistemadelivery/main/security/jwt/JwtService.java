package com.sistemadelivery.main.security.jwt;

import com.sistemadelivery.main.entity.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Generación y validación de tokens JWT firmados (HS256).
 * La clave secreta se inyecta desde la variable de entorno JWT_SECRET
 * y jamás se registra en logs ni se incluye en respuestas.
 */
@Service
public class JwtService {

    private static final int LONGITUD_MINIMA_SECRETO = 32;

    private final String secreto;
    private final long expiracionMs;

    private SecretKey clave;

    public JwtService(@Value("${app.jwt.secret}") String secreto,
                      @Value("${app.jwt.expiration-ms:86400000}") long expiracionMs) {
        this.secreto = secreto;
        this.expiracionMs = expiracionMs;
    }

    @PostConstruct
    void validarConfiguracion() {
        if (secreto == null || secreto.isBlank() || secreto.length() < LONGITUD_MINIMA_SECRETO) {
            throw new IllegalStateException(
                    "JWT_SECRET inválido: defina la variable de entorno JWT_SECRET con al menos "
                            + LONGITUD_MINIMA_SECRETO + " caracteres (ver .env.example)");
        }
        if (expiracionMs <= 0) {
            throw new IllegalStateException("JWT_EXPIRATION_MS debe ser un valor positivo en milisegundos");
        }
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
    }

    /** Emite un token firmado cuyo subject es el email del usuario. */
    public String generarToken(Usuario usuario) {
        long ahora = System.currentTimeMillis();
        return Jwts.builder()
                .subject(usuario.getEmail())
                .id(String.valueOf(usuario.getId()))
                .claim("rol", usuario.getRol().name())
                .issuedAt(new Date(ahora))
                .expiration(new Date(ahora + expiracionMs))
                .signWith(clave)
                .compact();
    }

    /**
     * Valida firma y expiración, y devuelve el email (subject).
     * Lanza {@link JwtException} si la firma es inválida o el token expiró.
     */
    public String extraerEmail(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(clave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    public long getExpiracionMs() {
        return expiracionMs;
    }
}
