package com.sistemadelivery.main.repository;


import com.sistemadelivery.main.entity.Comercio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComercioRepository extends JpaRepository<Comercio, Long> {
    Page<Comercio> findByActivoTrueAndAbiertoTrue(Pageable pageable);
    Page<Comercio> findByActivoTrue(Pageable pageable);
}
