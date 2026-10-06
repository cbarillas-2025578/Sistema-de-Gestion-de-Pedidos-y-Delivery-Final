package com.sistemadelivery.main.service.impl;

import com.sistemadelivery.main.dto.request.ComercioRequest;
import com.sistemadelivery.main.dto.request.EstadoComercioRequest;
import com.sistemadelivery.main.dto.response.ComercioResponse;
import com.sistemadelivery.main.dto.response.PageResponse;
import com.sistemadelivery.main.entity.Comercio;
import com.sistemadelivery.main.entity.enums.CategoriaComercio;
import com.sistemadelivery.main.exception.BusinessException;
import com.sistemadelivery.main.exception.ResourceNotFoundException;
import com.sistemadelivery.main.mapper.ComercioMapper;
import com.sistemadelivery.main.repository.ComercioRepository;
import com.sistemadelivery.main.service.AuditoriaService;
import com.sistemadelivery.main.service.ComercioService;
import com.sistemadelivery.main.util.SeguridadUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ComercioServiceImpl implements ComercioService {

    private final ComercioRepository comercioRepository;
    private final ComercioMapper mapper;
    private final AuditoriaService auditoriaService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ComercioResponse> listarAbiertos(int page, int size) {
        Page<Comercio> comercios = comercioRepository.findByActivoTrueAndAbiertoTrue(
                PageRequest.of(page, size, Sort.by("nombre")));
        return PageResponse.de(comercios.map(mapper::aRespuesta));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ComercioResponse> listarParaGestion(Boolean activo, Boolean abierto,
                                                            CategoriaComercio categoria, String q,
                                                            int page, int size) {
        Page<Comercio> comercios = comercioRepository.buscar(activo, abierto, categoria,
                (q == null || q.isBlank()) ? null : q.trim(),
                PageRequest.of(page, size, Sort.by("nombre")));
        return PageResponse.de(comercios.map(mapper::aRespuesta));
    }

    @Override
    @Transactional(readOnly = true)
    public ComercioResponse obtener(Long id) {
        Comercio comercio = comercioRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.de("Comercio", id));
        // Un comercio inactivo solo es visible para administradores.
        if (!Boolean.TRUE.equals(comercio.getActivo()) && !SeguridadUtil.esAdmin()) {
            throw ResourceNotFoundException.de("Comercio", id);
        }
        return mapper.aRespuesta(comercio);
    }

    @Override
    @Transactional
    public ComercioResponse crear(ComercioRequest request) {
        Comercio comercio = Comercio.builder()
                .nombre(request.nombre().trim())
                .categoria(request.categoria())
                .direccion(request.direccion().trim())
                .abierto(true)
                .activo(true)
                .build();
        comercio = comercioRepository.save(comercio);

        auditoriaService.registrar("COMERCIO_CREAR", "Comercio", comercio.getId(),
                "Comercio creado: " + comercio.getNombre());
        return mapper.aRespuesta(comercio);
    }

    @Override
    @Transactional
    public ComercioResponse actualizar(Long id, ComercioRequest request) {
        Comercio comercio = comercioRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.de("Comercio", id));

        String anterior = comercio.getNombre();
        comercio.setNombre(request.nombre().trim());
        comercio.setCategoria(request.categoria());
        comercio.setDireccion(request.direccion().trim());
        comercio = comercioRepository.save(comercio);

        auditoriaService.registrar("COMERCIO_ACTUALIZAR", "Comercio", id,
                "Nombre: " + anterior + " → " + comercio.getNombre());
        return mapper.aRespuesta(comercio);
    }

    @Override
    @Transactional
    public ComercioResponse cambiarEstado(Long id, EstadoComercioRequest request) {
        if (request.activo() == null && request.abierto() == null) {
            throw new BusinessException("ESTADO_REQUERIDO",
                    "Debe indicar al menos uno de los campos 'activo' u 'abierto'");
        }
        Comercio comercio = comercioRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.de("Comercio", id));

        StringBuilder cambios = new StringBuilder();
        if (request.activo() != null) {
            cambios.append("activo: ").append(comercio.getActivo()).append(" → ").append(request.activo());
            comercio.setActivo(request.activo());
        }
        if (request.abierto() != null) {
            if (cambios.length() > 0) {
                cambios.append("; ");
            }
            cambios.append("abierto: ").append(comercio.getAbierto()).append(" → ").append(request.abierto());
            comercio.setAbierto(request.abierto());
        }
        comercio = comercioRepository.save(comercio);

        auditoriaService.registrar("COMERCIO_ESTADO", "Comercio", id, cambios.toString());
        return mapper.aRespuesta(comercio);
    }
}
