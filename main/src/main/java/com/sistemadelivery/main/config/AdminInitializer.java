package com.sistemadelivery.main.config;

import com.sistemadelivery.main.entity.Usuario;
import com.sistemadelivery.main.entity.enums.Rol;
import com.sistemadelivery.main.repository.UsuarioRepository;
import com.sistemadelivery.main.service.AuditoriaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Inicialización segura del primer administrador (§5.1).
 * Solo se crea si ADMIN_EMAIL y ADMIN_PASSWORD están definidos y todavía
 * no existe ningún ADMIN. La contraseña nunca se registra en logs.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService;

    @Value("${app.admin.email:}")
    private String emailAdmin;

    @Value("${app.admin.password:}")
    private String passwordAdmin;

    @Override
    public void run(ApplicationArguments args) {
        if (emailAdmin == null || emailAdmin.isBlank() || passwordAdmin == null || passwordAdmin.isBlank()) {
            log.info("ADMIN_EMAIL/ADMIN_PASSWORD no definidos: no se crea administrador inicial.");
            return;
        }
        if (usuarioRepository.countByRol(Rol.ADMIN) > 0) {
            log.info("Ya existe un administrador: no se aplica la inicialización.");
            return;
        }

        String email = emailAdmin.trim().toLowerCase(Locale.ROOT);
        if (usuarioRepository.existsByEmail(email)) {
            log.warn("ADMIN_EMAIL ya corresponde a otro usuario: no se crea el administrador inicial.");
            return;
        }
        if (passwordAdmin.length() < 8) {
            log.error("ADMIN_PASSWORD debe tener al menos 8 caracteres: no se crea el administrador inicial.");
            return;
        }

        Usuario admin = Usuario.builder()
                .nombre("Administrador")
                .email(email)
                .password(passwordEncoder.encode(passwordAdmin))
                .rol(Rol.ADMIN)
                .activo(true)
                .build();
        admin = usuarioRepository.save(admin);

        auditoriaService.registrar("sistema", "ADMIN_INICIAL", "Usuario", admin.getId(),
                "Administrador inicial creado mediante variables de entorno");
        log.info("Administrador inicial creado para {}", email);
    }
}
