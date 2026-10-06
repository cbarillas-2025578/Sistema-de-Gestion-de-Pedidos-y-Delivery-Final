package com.sistemadelivery.main.repository;

import com.sistemadelivery.main.entity.Producto;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    /** Catálogo público: solo productos disponibles y con existencias. */
    Page<Producto> findByComercioIdAndDisponibleTrueAndStockGreaterThan(Long comercioId, Integer minStock, Pageable pageable);

    /** Gestión administrativa: todos los productos del comercio. */
    Page<Producto> findByComercioId(Long comercioId, Pageable pageable);

    /**
     * Bloqueo pesado de los productos involucrados en un pedido.
     * El ORDER BY id ASC fija un orden de bloqueo estable y evita interbloqueos
     * cuando varias transacciones piden los mismos productos en distinto orden.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Producto p WHERE p.id IN :ids ORDER BY p.id ASC")
    List<Producto> findAllByIdInForUpdateOrderByIdAsc(@Param("ids") List<Long> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Producto p WHERE p.id = :id")
    Optional<Producto> findByIdForUpdate(@Param("id") Long id);
}
