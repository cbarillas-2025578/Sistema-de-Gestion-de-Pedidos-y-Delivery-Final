package com.sistemadelivery.main.dto.response;

import java.math.BigDecimal;

/**
 * Precio unitario y subtotal HISTÓRICOS: se congelaron al confirmar el pedido
 * y no cambian aunque el catálogo modifique sus precios.
 */
public record DetallePedidoResponse(Long productoId,
                                    String productoNombre,
                                    Integer cantidad,
                                    BigDecimal precioUnitario,
                                    BigDecimal subtotal) {
}
