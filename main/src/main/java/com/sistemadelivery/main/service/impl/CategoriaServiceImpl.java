package com.sistemadelivery.main.service.impl;

import com.sistemadelivery.main.dto.request.CategoriaRequest;
import com.sistemadelivery.main.dto.response.CategoriaResponse;
import com.sistemadelivery.main.entity.Categoria;
import com.sistemadelivery.main.exception.ConflictException;
import com.sistemadelivery.main.exception.ResourceNotFoundException;
import com.sistemadelivery.main.mapper.CategoriaMapper;
import com.sistemadelivery.main.repository.CategoriaRepository;
import com.sistemadelivery.main.service.AuditoriaService;
import com.sistemadelivery.main.service.CategoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CategoriaServiceImpl implements CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final CategoriaMapper mapper;
    private final AuditoriaService auditoriaService;

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaResponse> listarActivas() {
        return categoriaRepository.findByActivoTrue().stream()
                .map(mapper::aRespuesta)
                .toList();
    }

    @Override
    @Transactional
    public CategoriaResponse crear(CategoriaRequest request) {
        String nombre = request.nombre().trim();
        if (categoriaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ConflictException("CATEGORIA_DUPLICADA", "Ya existe una categoría con ese nombre");
        }
        Categoria categoria = Categoria.builder()
                .nombre(nombre)
                .descripcion(request.descripcion())
                .activo(true)
                .build();
        categoria = categoriaRepository.save(categoria);
        auditoriaService.registrar("CATEGORIA_CREAR", "Categoria", categoria.getId(),
                "Categoría creada: " + nombre);
        return mapper.aRespuesta(categoria);
    }

    @Override
    @Transactional
    public CategoriaResponse actualizar(Long id, CategoriaRequest request) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.de("Categoría", id));

        String nombre = request.nombre().trim();
        categoriaRepository.findByNombreIgnoreCase(nombre)
                .filter(existente -> !existente.getId().equals(id))
                .ifPresent(duplicada -> {
                    throw new ConflictException("CATEGORIA_DUPLICADA", "Ya existe una categoría con ese nombre");
                });

        String anterior = categoria.getNombre();
        categoria.setNombre(nombre);
        categoria.setDescripcion(request.descripcion());
        categoria = categoriaRepository.save(categoria);

        auditoriaService.registrar("CATEGORIA_ACTUALIZAR", "Categoria", id,
                "Nombre: " + anterior + " → " + nombre);
        return mapper.aRespuesta(categoria);
    }

    @Override
    @Transactional
    public CategoriaResponse cambiarEstado(Long id, boolean activo) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.de("Categoría", id));
        boolean anterior = Boolean.TRUE.equals(categoria.getActivo());
        categoria.setActivo(activo);
        categoria = categoriaRepository.save(categoria);

        auditoriaService.registrar("CATEGORIA_ESTADO", "Categoria", id,
                "activo: " + anterior + " → " + activo);
        return mapper.aRespuesta(categoria);
    }
}
