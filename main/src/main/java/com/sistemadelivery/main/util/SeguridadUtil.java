package com.sistemadelivery.main.util;

import com.sistemadelivery.main.entity.enums.Rol;
import com.sistemadelivery.main.exception.UnauthorizedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Acceso a la identidad autenticada del hilo actual.
 * La identidad proviene SIEMPRE del SecurityContext (token JWT validado),
 * nunca de datos enviados por el cliente en el cuerpo de la solicitud.
 */
public final class SeguridadUtil {

    private SeguridadUtil() {
    }

    /** Email del usuario autenticado; lanza 401 si no hay sesión válida. */
    public static String emailActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || auth instanceof AnonymousAuthenticationToken
                || auth.getName() == null || auth.getName().isBlank()) {
            throw new UnauthorizedException("NO_AUTENTICADO", "Se requiere una autenticación válida");
        }
        return auth.getName();
    }

    /** Verifica que el usuario autenticado posea el rol indicado. */
    public static boolean esRol(Rol rol) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return false;
        }
        String esperado = "ROLE_" + rol.name();
        return auth.getAuthorities() != null
                && auth.getAuthorities().stream().anyMatch(a -> esperado.equals(a.getAuthority()));
    }

    public static boolean esAdmin() {
        return esRol(Rol.ADMIN);
    }
}
