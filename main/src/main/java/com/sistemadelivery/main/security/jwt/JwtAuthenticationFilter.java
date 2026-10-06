package com.sistemadelivery.main.security.jwt;

import com.sistemadelivery.main.repository.UsuarioRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtro stateless: valida firma/expiración del JWT y reconstruye la autenticación
 * en cada petición. El rol y el estado del usuario se leen de la BASE DE DATOS
 * (no del token), de modo que un rol revocado o un usuario desactivado
 * pierden acceso de inmediato. El token nunca se registra en logs.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = extraerToken(request);
            if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                String email = jwtService.extraerEmail(token);
                usuarioRepository.findByEmail(email)
                        .filter(u -> Boolean.TRUE.equals(u.getActivo()))
                        .ifPresent(usuario -> {
                            var autoridades = List.of(
                                    new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name()));
                            var autenticacion = new UsernamePasswordAuthenticationToken(
                                    usuario.getEmail(), null, autoridades);
                            autenticacion.setDetails(
                                    new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(autenticacion);
                        });
            }
        } catch (JwtException | IllegalArgumentException e) {
            // Sin contenido del token en el log: solo la causa técnica.
            log.debug("Token JWT inválido o expirado: {}", e.getClass().getSimpleName());
        }
        filterChain.doFilter(request, response);
    }

    /**
     * Cabecera Authorization estándar. Como alternativa limitada al endpoint SSE
     * de seguimiento se acepta el parámetro {@code access_token}, porque el
     * objeto EventSource del navegador no puede enviar cabeceras personalizadas.
     */
    private String extraerToken(HttpServletRequest request) {
        String cabecera = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (cabecera != null && cabecera.startsWith("Bearer ")) {
            return cabecera.substring(7).trim();
        }
        if (request.getRequestURI() != null && request.getRequestURI().endsWith("/seguimiento")) {
            String parametro = request.getParameter("access_token");
            if (parametro != null && !parametro.isBlank()) {
                return parametro.trim();
            }
        }
        return null;
    }
}
