package com.sistemadelivery.main.repository;

import com.sistemadelivery.main.entity.Pedido;
import com.sistemadelivery.main.entity.enums.EstadoPedido;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    Page<Pedido> findByClienteId(Long clienteId, Pageable pageable);

    Page<Pedido> findByRepartidorId(Long repartidorId, Pageable pageable);

    Page<Pedido> findByEstado(EstadoPedido estado, Pageable pageable);

    Page<Pedido> findByRepartidorNullAndEstado(EstadoPedido estado, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Pedido p WHERE p.id = :id")
    Optional<Pedido> findByIdForUpdate(@Param("id") Long id);
}