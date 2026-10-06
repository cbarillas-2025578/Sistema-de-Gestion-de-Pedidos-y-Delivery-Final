package com.sistemadelivery.main.repository;

import com.sistemadelivery.main.entity.Usuario;
import com.sistemadelivery.main.entity.enums.Rol;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByRol(Rol rol);

    Page<Usuario> findByRol(Rol rol, Pageable pageable);

    Page<Usuario> findByRolAndActivoTrue(Rol rol, Pageable pageable);
}
