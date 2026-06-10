package com.upb.ecommerce.domain.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "pedidos")
@Data
@NoArgsConstructor
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tienda_id", nullable = false)
    private Tienda tienda;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "direccion_id")
    private DireccionEnvio direccionEnvio;

    @Column(name = "codigo_seguimiento", unique = true, length = 100)
    private String codigoSeguimiento;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Column(name = "estado_pedido", nullable = false, length = 50)
    private String estadoPedido = "PENDIENTE"; // PENDIENTE, PAGADO, PREPARANDO, ENVIADO, ENTREGADO, CANCELADO

    // Nullable a propósito: los pedidos creados con el .backup no tienen fecha.
    // Los pedidos nuevos la reciben en la construcción (usada por dashboard y reportes).
    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetallePedido> detalles;
}
