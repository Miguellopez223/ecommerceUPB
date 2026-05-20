package com.upb.ecommerce.repository.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "pagos")
@Data
@NoArgsConstructor
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @Column(nullable = false, length = 50)
    private String metodo; // Ej: "QR", "TARJETA"

    @Column(name = "transaccion_pasarela_id", length = 255)
    private String transaccionPasarelaId; // El código que devuelva el banco/pasarela

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(name = "estado_pago", nullable = false, length = 50)
    private String estadoPago = "PENDIENTE"; // PENDIENTE, EXITOSO, RECHAZADO
}