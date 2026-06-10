package com.upb.ecommerce.domain.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Unidad de medida de un producto (ej: "Unidad" / "un", "Kilogramo" / "kg").
 * Pertenece a una tienda (multi-tenant) y se borra de forma lógica con {@code estado}.
 */
@Entity
@Table(name = "unidades_medida", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"tienda_id", "nombre"})
})
@Data
@NoArgsConstructor
public class UnidadMedida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tienda_id", nullable = false)
    private Tienda tienda;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 20)
    private String abreviatura;

    @Column(nullable = false)
    private Boolean estado = true;
}
