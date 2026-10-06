package com.sistemadelivery.main.service.impl;

import com.sistemadelivery.main.dto.response.AuditoriaResponse;
import com.sistemadelivery.main.dto.response.PageResponse;
import com.sistemadelivery.main.entity.RegistroAuditoria;
import com.sistemadelivery.main.mapper.AuditoriaMapper;
import com.sistemadelivery.main.repository.RegistroAuditoriaRepository;
import com.sistemadelivery.main.service.AuditoriaService;
import com.sistemadelivery.main.util.SeguridadUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Escribe dentro de la transacción de la operación que audita:
 * si la operación se revierte, su registro de auditoría también.
 * Nunca almacena contraseñas, tokens ni secretos.
 */
@Service
@RequiredArgsConstructor
public class AuditoriaServiceImpl implements AuditoriaService {

    private final RegistroAuditoriaRepository registroRepository;
    private final AuditoriaMapper mapper;

    @Override
    @Transactional
    public void registrar(String usuario, String accion, String entidad, Long entidadId, String detalles) {
        registroRepository.save(RegistroAuditoria.builder()
                .usuario(usuario)
                .accion(accion)
                .entidad(entidad)
                .entidadId(entidadId)
                .detalles(detalles)
                .build());
    }

    @Override
    @Transactional
    public void registrar(String accion, String entidad, Long entidadId, String detalles) {
        String usuario;
        try {
            usuario = SeguridadUtil.emailActual();
        } catch (RuntimeException e) {
            usuario = "sistema";
        }
        registrar(usuario, accion, entidad, entidadId, detalles);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuditoriaResponse> buscar(String usuario, String accion, String entidad, Long entidadId,
                                                  LocalDateTime desde, LocalDateTime hasta, int page, int size) {
        Page<RegistroAuditoria> registros = registroRepository.buscar(
                normalizar(usuario),
                accion == null ? null : accion.trim().toUpperCase(Locale.ROOT),
                entidad == null ? null : entidad.trim().toUpperCase(Locale.ROOT),
                entidadId,
                desde,
                hasta,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fecha")));

        return PageResponse.de(registros.map(mapper::aRespuesta));
    }

    private String normalizar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim().toLowerCase(Locale.ROOT);
    }
}
