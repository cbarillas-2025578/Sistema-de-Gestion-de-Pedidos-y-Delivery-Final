package com.sistemadelivery.main.service.impl;

import com.sistemadelivery.main.dto.response.EstadisticasResponse;
import com.sistemadelivery.main.entity.enums.EstadoPedido;
import com.sistemadelivery.main.repository.PedidoRepository;
import com.sistemadelivery.main.service.EstadisticaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EstadisticaServiceImpl implements EstadisticaService {

    private final PedidoRepository pedidoRepository;

    @Override
    @Transactional(readOnly = true)
    public EstadisticasResponse obtener() {
        long totalPedidos = pedidoRepository.count();

        Map<EstadoPedido, Long> porEstado = new EnumMap<>(EstadoPedido.class);
        for (EstadoPedido estado : EstadoPedido.values()) {
            porEstado.put(estado, 0L);
        }
        for (Object[] fila : pedidoRepository.contarPorEstado()) {
            porEstado.put((EstadoPedido) fila[0], (Long) fila[1]);
        }

        BigDecimal ventas = nuloAScala2(pedidoRepository.sumarMontoPorEstado(EstadoPedido.ENTREGADO));
        long entregados = pedidoRepository.countByEstado(EstadoPedido.ENTREGADO);
        BigDecimal ticketPromedio = entregados == 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : ventas.divide(BigDecimal.valueOf(entregados), 2, RoundingMode.HALF_UP);

        return new EstadisticasResponse(totalPedidos, porEstado, ventas, ticketPromedio);
    }

    private BigDecimal nuloAScala2(BigDecimal valor) {
        return (valor == null ? BigDecimal.ZERO : valor).setScale(2, RoundingMode.HALF_UP);
    }
}
