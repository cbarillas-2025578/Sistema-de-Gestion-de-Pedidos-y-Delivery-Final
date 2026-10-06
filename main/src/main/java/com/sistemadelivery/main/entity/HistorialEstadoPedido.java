package com.sistemadelivery.main.entity;

import com.sistemadelivery.main.entity.enums.EstadoPedido;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Registro inmutable de cada cambio de estado de un pedido.
 * Permite el seguimiento histórico (§11) sin sobrescribir información.
 */
@Entity
@Table(name = "historial_estado_pedido",
        indexes = @Index(name = "idx_historial_pedido_fecha", columnList = "pedido_id, fecha"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistorialEstadoPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPedido estado;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", length = 20)
    private EstadoPedido estadoAnterior;

    /** Email del usuario que ejecutó la transición (nunca contraseñas ni tokens). */
    @Column(nullable = false, length = 100)
    private String usuario;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime fecha;
}
