package com.sistemadelivery.main.repository;

import com.sistemadelivery.main.entity.HistorialEstadoPedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistorialEstadoPedidoRepository extends JpaRepository<HistorialEstadoPedido, Long> {

    List<HistorialEstadoPedido> findByPedidoIdOrderByFechaAscIdAsc(Long pedidoId);
}
