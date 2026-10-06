package com.sistemadelivery.main.repository;

import com.sistemadelivery.main.entity.RegistroAuditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface RegistroAuditoriaRepository extends JpaRepository<RegistroAuditoria, Long> {

    Page<RegistroAuditoria> findByFechaBetween(LocalDateTime inicio, LocalDateTime fin, Pageable pageable);

    Page<RegistroAuditoria> findByEntidadAndEntidadId(String entidad, Long entidadId, Pageable pageable);
}