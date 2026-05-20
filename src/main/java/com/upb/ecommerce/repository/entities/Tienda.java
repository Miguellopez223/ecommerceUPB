package com.upb.ecommerce.repository.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Entity
@Table(name = "tiendas")
@Data
@NoArgsConstructor
public class Tienda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(unique = true, nullable = false, length = 150)
    private String slug;

    @Column(name = "telefono_contacto", length = 50)
    private String telefonoContacto;

    @Column(name = "email_contacto", length = 150)
    private String emailContacto;

    @Column(name = "logo_url", columnDefinition = "TEXT")
    private String logoUrl;

    @Column(nullable = false)
    private Boolean estado = true;

    // Relación bidireccional: Una tienda tiene muchos usuarios
    @OneToMany(mappedBy = "tienda", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Usuario> usuarios;
}