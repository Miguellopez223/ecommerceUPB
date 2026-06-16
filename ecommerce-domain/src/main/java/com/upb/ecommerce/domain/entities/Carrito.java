package com.upb.ecommerce.domain.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "carritos")
@Data
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Carrito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tienda_id", nullable = false)
    private Tienda tienda;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "total_estimado", precision = 10, scale = 2)
    private BigDecimal totalEstimado = BigDecimal.ZERO;

    @Column(nullable = false, length = 50)
    private String estado = "ACTIVO"; // ACTIVO, CONVERTIDO_A_PEDIDO, ABANDONADO

    /**
     * Fecha de la última modificación del carrito. La rellena sola la auditoría de JPA
     * ({@code @LastModifiedDate}) en cada save. Se usa para detectar carritos abandonados:
     * un carrito ACTIVO cuya fechaActualizacion es muy antigua = el cliente no continuó.
     * Nullable a propósito: las filas creadas con el .backup no tienen valor.
     */
    @LastModifiedDate
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @OneToMany(mappedBy = "carrito", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleCarrito> detalles;
}
