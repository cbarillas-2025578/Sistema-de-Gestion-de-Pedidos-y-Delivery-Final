package com.sistemadelivery.main.repository;

import com.sistemadelivery.main.entity.Comercio;
import com.sistemadelivery.main.entity.enums.CategoriaComercio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ComercioRepository extends JpaRepository<Comercio, Long> {

    Page<Comercio> findByActivoTrueAndAbiertoTrue(Pageable pageable);

    Page<Comercio> findByActivoTrue(Pageable pageable);

    /** Listado de gestión (solo ADMIN) con filtros opcionales combinables. */
    @Query("""
            SELECT c FROM Comercio c
            WHERE (:activo IS NULL OR c.activo = :activo)
              AND (:abierto IS NULL OR c.abierto = :abierto)
              AND (:categoria IS NULL OR c.categoria = :categoria)
              AND (:q IS NULL OR LOWER(c.nombre) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<Comercio> buscar(@Param("activo") Boolean activo,
                          @Param("abierto") Boolean abierto,
                          @Param("categoria") CategoriaComercio categoria,
                          @Param("q") String q,
                          Pageable pageable);
}
