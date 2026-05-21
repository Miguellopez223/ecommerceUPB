package com.upb.ecommerce.domain.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "direcciones_envio")
@Data
@NoArgsConstructor
public class DireccionEnvio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "direccion_calle", nullable = false, columnDefinition = "TEXT")
    private String direccionCalle;

    @Column(nullable = false, length = 100)
    private String ciudad;

    @Column(columnDefinition = "TEXT")
    private String referencias;

    @Column(nullable = false)
    private Boolean estado = true;
}
