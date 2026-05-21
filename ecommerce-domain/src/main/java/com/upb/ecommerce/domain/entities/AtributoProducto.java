package com.upb.ecommerce.domain.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "atributo_productos")
@Data
@NoArgsConstructor
public class AtributoProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false, length = 100)
    private String nombre; // Ej: "Color", "Talla", "Material"

    @Column(nullable = false, length = 100)
    private String valor;  // Ej: "Azul", "XL", "Acero"
}
