package com.upb.ecommerce.repository.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "usuarios", uniqueConstraints = {
        // Garantiza que un email no se repita dentro de la MISMA tienda
        @UniqueConstraint(columnNames = {"tienda_id", "email"})
})
@Data
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación: Muchos usuarios pertenecen a una tienda
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tienda_id", nullable = false)
    private Tienda tienda;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 50)
    private String rol; // Valores esperados: "ADMIN" o "CLIENTE"

    @Column(nullable = false)
    private Boolean estado = true;
}