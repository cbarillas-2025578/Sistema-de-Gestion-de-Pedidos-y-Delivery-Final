package com.sistemadelivery.main.repository;

import com.sistemadelivery.main.entity.RegistroAuditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface RegistroAuditoriaRepository extends JpaRepository<RegistroAuditoria, Long> {

    /**
     * Búsqueda con filtros combinables para la vista administrativa de auditoría.
     * Cada parámetro aparece en una expresión tipada y en una comprobación IS NULL.
     */
    @Query("""
            SELECT r FROM RegistroAuditoria r
            WHERE (:usuario IS NULL OR LOWER(r.usuario) = LOWER(:usuario))
              AND (:accion IS NULL OR r.accion = :accion)
              AND (:entidad IS NULL OR r.entidad = :entidad)
              AND (:entidadId IS NULL OR r.entidadId = :entidadId)
              AND (:desde IS NULL OR r.fecha >= :desde)
              AND (:hasta IS NULL OR r.fecha <= :hasta)
            """)
    Page<RegistroAuditoria> buscar(@Param("usuario") String usuario,
                                   @Param("accion") String accion,
                                   @Param("entidad") String entidad,
                                   @Param("entidadId") Long entidadId,
                                   @Param("desde") LocalDateTime desde,
                                   @Param("hasta") LocalDateTime hasta,
                                   Pageable pageable);
}
