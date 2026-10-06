package com.sistemadelivery.main.mapper;

import com.sistemadelivery.main.dto.response.DetallePedidoResponse;
import com.sistemadelivery.main.dto.response.HistorialEstadoResponse;
import com.sistemadelivery.main.dto.response.PedidoResponse;
import com.sistemadelivery.main.entity.DetallePedido;
import com.sistemadelivery.main.entity.HistorialEstadoPedido;
import com.sistemadelivery.main.entity.Pedido;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Convierte pedidos a DTO de salida. Debe usarse dentro de un método
 * transaccional del servicio porque las asociaciones se cargan de forma perezosa.
 */
@Component
public class PedidoMapper {

    public PedidoResponse aRespuesta(Pedido pedido) {
        List<DetallePedidoResponse> detalles = pedido.getDetalles().stream()
                .map(this::aRespuesta)
                .toList();
        return new PedidoResponse(
                pedido.getId(),
                pedido.getCliente() != null ? pedido.getCliente().getId() : null,
                pedido.getCliente() != null ? pedido.getCliente().getNombre() : null,
                pedido.getRepartidor() != null ? pedido.getRepartidor().getId() : null,
                pedido.getRepartidor() != null ? pedido.getRepartidor().getNombre() : null,
                pedido.getComercio() != null ? pedido.getComercio().getId() : null,
                pedido.getComercio() != null ? pedido.getComercio().getNombre() : null,
                pedido.getFechaPedido(),
                pedido.getCostoEnvio(),
                pedido.getMontoTotal(),
                pedido.getEstado(),
                detalles,
                pedido.getVersion());
    }

    public DetallePedidoResponse aRespuesta(DetallePedido detalle) {
        return new DetallePedidoResponse(
                detalle.getProducto().getId(),
                detalle.getProducto().getNombre(),
                detalle.getCantidad(),
                detalle.getPrecioUnitario(),
                detalle.getSubtotal());
    }

    public HistorialEstadoResponse aRespuesta(HistorialEstadoPedido registro) {
        return new HistorialEstadoResponse(
                registro.getId(),
                registro.getEstado(),
                registro.getEstadoAnterior(),
                registro.getUsuario(),
                registro.getFecha());
    }
}
