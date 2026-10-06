package com.sistemadelivery.main.repository;

import com.sistemadelivery.main.entity.Pedido;
import com.sistemadelivery.main.entity.enums.EstadoPedido;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    Page<Pedido> findByClienteId(Long clienteId, Pageable pageable);

    Page<Pedido> findByRepartidorId(Long repartidorId, Pageable pageable);

    /** Pedidos en estados PENDIENTE o EN_PREPARACION sin repartidor asignado. */
    Page<Pedido> findByRepartidorNullAndEstadoIn(EstadoPedido[] estados, Pageable pageable);

    /** Bloqueo pesado de la fila del pedido para operaciones de estado/asignación. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Pedido p WHERE p.id = :id")
    Optional<Pedido> findByIdForUpdate(@Param("id") Long id);

    /**
     * Listado administrativo con filtros combinables.
     * Cada parámetro participa en una comparación tipada y en una comprobación IS NULL,
     * de modo que Hibernate siempre infiere el tipo correcto para PostgreSQL.
     */
    @Query("""
            SELECT p FROM Pedido p
            WHERE (:estado IS NULL OR p.estado = :estado)
              AND (:clienteId IS NULL OR p.cliente.id = :clienteId)
              AND (:repartidorId IS NULL OR p.repartidor.id = :repartidorId)
              AND (:desde IS NULL OR p.fechaPedido >= :desde)
              AND (:hasta IS NULL OR p.fechaPedido <= :hasta)
            """)
    Page<Pedido> buscar(@Param("estado") EstadoPedido estado,
                        @Param("clienteId") Long clienteId,
                        @Param("repartidorId") Long repartidorId,
                        @Param("desde") LocalDateTime desde,
                        @Param("hasta") LocalDateTime hasta,
                        Pageable pageable);

    @Query("SELECT p.estado, COUNT(p) FROM Pedido p GROUP BY p.estado")
    List<Object[]> contarPorEstado();

    @Query("SELECT COALESCE(SUM(p.montoTotal), 0) FROM Pedido p WHERE p.estado = :estado")
    BigDecimal sumarMontoPorEstado(@Param("estado") EstadoPedido estado);

    long countByEstado(EstadoPedido estado);

    Page<Pedido> findByRepartidorNullAndEstado(EstadoPedido estadoPedido, PageRequest fechaPedido);
}
