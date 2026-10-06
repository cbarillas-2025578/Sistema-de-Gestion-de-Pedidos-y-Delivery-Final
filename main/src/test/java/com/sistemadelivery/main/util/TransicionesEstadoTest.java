package com.sistemadelivery.main.util;

import com.sistemadelivery.main.entity.enums.EstadoPedido;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica la máquina de estados del pedido (§6.4):
 * PENDIENTE → EN_PREPARACION | CANCELADO
 * EN_PREPARACION → EN_CAMINO
 * EN_CAMINO → ENTREGADO
 * ENTREGADO y CANCELADO son estados finales.
 */
class TransicionesEstadoTest {

    @Test
    void permiteLasTransicionesValidas() {
        assertThat(TransicionesEstado.esPermitida(EstadoPedido.PENDIENTE, EstadoPedido.EN_PREPARACION)).isTrue();
        assertThat(TransicionesEstado.esPermitida(EstadoPedido.PENDIENTE, EstadoPedido.CANCELADO)).isTrue();
        assertThat(TransicionesEstado.esPermitida(EstadoPedido.EN_PREPARACION, EstadoPedido.EN_CAMINO)).isTrue();
        assertThat(TransicionesEstado.esPermitida(EstadoPedido.EN_CAMINO, EstadoPedido.ENTREGADO)).isTrue();
    }

    @Test
    void rechazaSaltosDeEstado() {
        assertThat(TransicionesEstado.esPermitida(EstadoPedido.PENDIENTE, EstadoPedido.EN_CAMINO)).isFalse();
        assertThat(TransicionesEstado.esPermitida(EstadoPedido.PENDIENTE, EstadoPedido.ENTREGADO)).isFalse();
        assertThat(TransicionesEstado.esPermitida(EstadoPedido.EN_PREPARACION, EstadoPedido.ENTREGADO)).isFalse();
        assertThat(TransicionesEstado.esPermitida(EstadoPedido.EN_PREPARACION, EstadoPedido.CANCELADO)).isFalse();
        assertThat(TransicionesEstado.esPermitida(EstadoPedido.EN_CAMINO, EstadoPedido.CANCELADO)).isFalse();
    }

    @Test
    void losEstadosFinalesNoTransicionan() {
        for (EstadoPedido destino : EstadoPedido.values()) {
            assertThat(TransicionesEstado.esPermitida(EstadoPedido.ENTREGADO, destino)).isFalse();
            assertThat(TransicionesEstado.esPermitida(EstadoPedido.CANCELADO, destino)).isFalse();
        }
    }

    @Test
    void rechazaAutoTransicionesYNulos() {
        for (EstadoPedido estado : EstadoPedido.values()) {
            assertThat(TransicionesEstado.esPermitida(estado, estado)).isFalse();
        }
        assertThat(TransicionesEstado.esPermitida(null, EstadoPedido.PENDIENTE)).isFalse();
        assertThat(TransicionesEstado.esPermitida(EstadoPedido.PENDIENTE, null)).isFalse();
        assertThat(TransicionesEstado.esPermitida(null, null)).isFalse();
    }
}