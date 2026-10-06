package com.sistemadelivery.main.service.impl;

import com.sistemadelivery.main.dto.request.DisponibilidadRequest;
import com.sistemadelivery.main.dto.request.ProductoRequest;
import com.sistemadelivery.main.dto.request.StockRequest;
import com.sistemadelivery.main.dto.response.PageResponse;
import com.sistemadelivery.main.dto.response.ProductoResponse;
import com.sistemadelivery.main.entity.Categoria;
import com.sistemadelivery.main.entity.Comercio;
import com.sistemadelivery.main.entity.Producto;
import com.sistemadelivery.main.exception.BusinessException;
import com.sistemadelivery.main.exception.ConflictException;
import com.sistemadelivery.main.exception.ForbiddenException;
import com.sistemadelivery.main.exception.ResourceNotFoundException;
import com.sistemadelivery.main.mapper.ProductoMapper;
import com.sistemadelivery.main.repository.CategoriaRepository;
import com.sistemadelivery.main.repository.ComercioRepository;
import com.sistemadelivery.main.repository.ProductoRepository;
import com.sistemadelivery.main.service.AuditoriaService;
import com.sistemadelivery.main.service.ProductoService;
import com.sistemadelivery.main.util.SeguridadUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository productoRepository;
    private final ComercioRepository comercioRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProductoMapper mapper;
    private final AuditoriaService auditoriaService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductoResponse> listarPorComercio(Long comercioId, boolean gestion, int page, int size) {
        Comercio comercio = comercioRepository.findById(comercioId)
                .orElseThrow(() -> ResourceNotFoundException.de("Comercio", comercioId));

        if (gestion) {
            if (!SeguridadUtil.esAdmin()) {
                throw new ForbiddenException("Solo un administrador puede ver el listado de gestión");
            }
            Page<Producto> productos = productoRepository.findByComercioId(comercioId,
                    PageRequest.of(page, size, Sort.by("id")));
            return PageResponse.de(productos.map(mapper::aRespuesta));
        }

        if (!Boolean.TRUE.equals(comercio.getActivo())) {
            throw ResourceNotFoundException.de("Comercio", comercioId);
        }
        Page<Producto> productos = productoRepository
                .findByComercioIdAndDisponibleTrueAndStockGreaterThan(comercioId, 0,
                        PageRequest.of(page, size, Sort.by("nombre")));
        return PageResponse.de(productos.map(mapper::aRespuesta));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoResponse obtener(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.de("Producto", id));
        boolean visible = Boolean.TRUE.equals(producto.getDisponible())
                && Boolean.TRUE.equals(producto.getComercio().getActivo());
        if (!visible && !SeguridadUtil.esAdmin()) {
            throw ResourceNotFoundException.de("Producto", id);
        }
        return mapper.aRespuesta(producto);
    }

    @Override
    @Transactional
    public ProductoResponse crear(Long comercioId, ProductoRequest request) {
        Comercio comercio = comercioRepository.findById(comercioId)
                .orElseThrow(() -> ResourceNotFoundException.de("Comercio", comercioId));

        Producto producto = Producto.builder()
                .comercio(comercio)
                .categoria(categoriaValida(request.categoriaId()))
                .nombre(request.nombre().trim())
                .descripcion(request.descripcion())
                .precio(request.precio())
                .stock(request.stock())
                .disponible(true)
                .build();

        producto = productoRepository.save(producto);
        auditoriaService.registrar("PRODUCTO_CREAR", "Producto", producto.getId(),
                "Producto creado: " + producto.getNombre() + ", precio " + producto.getPrecio());
        return mapper.aRespuesta(producto);
    }

    @Override
    @Transactional
    public ProductoResponse actualizar(Long id, ProductoRequest request) {
        Producto producto = productoRepository.findByIdForUpdate(id)
                .orElseThrow(() -> ResourceNotFoundException.de("Producto", id));

        BigDecimal precioAnterior = producto.getPrecio();
        producto.setNombre(request.nombre().trim());
        producto.setDescripcion(request.descripcion());
        producto.setPrecio(request.precio());
        if (request.categoriaId() != null) { // null = sin cambio
            producto.setCategoria(categoriaValida(request.categoriaId()));
        }
        producto = productoRepository.save(producto);

        auditoriaService.registrar("PRODUCTO_ACTUALIZAR", "Producto", id,
                "Precio: " + precioAnterior + " → " + producto.getPrecio()
                        + "; nombre: " + producto.getNombre()
                        + ". Los pedidos históricos conservan su precio propio.");
        return mapper.aRespuesta(producto);
    }

    @Override
    @Transactional
    public ProductoResponse ajustarStock(Long id, StockRequest request) {
        // Bloqueo de fila: evita perder una deducción concurrente de un pedido.
        Producto producto = productoRepository.findByIdForUpdate(id)
                .orElseThrow(() -> ResourceNotFoundException.de("Producto", id));

        Integer stockAnterior = producto.getStock();
        producto.setStock(request.stock());
        producto = productoRepository.save(producto);

        auditoriaService.registrar("PRODUCTO_STOCK", "Producto", id,
                "Stock: " + stockAnterior + " → " + producto.getStock());
        return mapper.aRespuesta(producto);
    }

    @Override
    @Transactional
    public void cambiarDisponibilidad(Long id, DisponibilidadRequest request) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.de("Producto", id));

        boolean anterior = Boolean.TRUE.equals(producto.getDisponible());
        producto.setDisponible(request.disponible());
        producto = productoRepository.save(producto);

        auditoriaService.registrar("PRODUCTO_DISPONIBILIDAD", "Producto", id,
                "disponible: " + anterior + " → " + producto.getDisponible());
    }

    private Categoria categoriaValida(Long categoriaId) {
        if (categoriaId == null) {
            return null;
        }
        Categoria categoria = categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new BusinessException("CATEGORIA_INVALIDA",
                        "La categoría indicada no existe"));
        if (!Boolean.TRUE.equals(categoria.getActivo())) {
            throw new ConflictException("CATEGORIA_INACTIVA", "La categoría indicada está inactiva");
        }
        return categoria;
    }
}
