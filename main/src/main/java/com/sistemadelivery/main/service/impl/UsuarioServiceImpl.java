package com.sistemadelivery.main.service.impl;

import com.sistemadelivery.main.dto.request.UsuarioCrearRequest;
import com.sistemadelivery.main.dto.response.PageResponse;
import com.sistemadelivery.main.dto.response.UsuarioResponse;
import com.sistemadelivery.main.entity.Usuario;
import com.sistemadelivery.main.entity.enums.Rol;
import com.sistemadelivery.main.exception.ConflictException;
import com.sistemadelivery.main.exception.ForbiddenException;
import com.sistemadelivery.main.exception.ResourceNotFoundException;
import com.sistemadelivery.main.mapper.UsuarioMapper;
import com.sistemadelivery.main.repository.UsuarioRepository;
import com.sistemadelivery.main.service.AuditoriaService;
import com.sistemadelivery.main.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper mapper;
    private final AuditoriaService auditoriaService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UsuarioResponse> listar(Rol rol, int page, int size) {
        PageRequest paginacion = PageRequest.of(page, size, Sort.by("fechaCreacion").descending());
        Page<Usuario> usuarios = rol == null
                ? usuarioRepository.findAll(paginacion)
                : usuarioRepository.findByRol(rol, paginacion);
        return PageResponse.de(usuarios.map(mapper::aRespuesta));
    }

    @Override
    @Transactional
    public UsuarioResponse crear(UsuarioCrearRequest request) {
        if (request.rol() == Rol.ADMIN) {
            throw new ForbiddenException(
                    "No se pueden crear administradores desde la API; use el mecanismo de inicialización "
                            + "seguro (ADMIN_EMAIL / ADMIN_PASSWORD)");
        }
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (usuarioRepository.existsByEmail(email)) {
            throw new ConflictException("EMAIL_DUPLICADO", "El correo electrónico ya está registrado");
        }

        Usuario usuario = Usuario.builder()
                .nombre(request.nombre().trim())
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .rol(request.rol())
                .direccion(request.direccion())
                .telefono(request.telefono())
                .activo(true)
                .build();
        usuario = usuarioRepository.save(usuario);

        auditoriaService.registrar("USUARIO_CREAR", "Usuario", usuario.getId(),
                "Usuario creado con rol " + usuario.getRol());
        return mapper.aRespuesta(usuario);
    }
}
