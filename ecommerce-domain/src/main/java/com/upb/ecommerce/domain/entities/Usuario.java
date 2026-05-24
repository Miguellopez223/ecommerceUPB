package com.upb.ecommerce.domain.entities;

import com.upb.ecommerce.domain.enums.RolType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "usuarios", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"tienda_id", "email"})
})
@Data
@NoArgsConstructor
public class Usuario implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tienda_id", nullable = false)
    private Tienda tienda;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private RolType rol;

    @Column(nullable = false)
    private Boolean estado = true;

    /**
     * Constructor parcial usado por la query optimizada
     * {@code findByIdToValidateSession} en JWT validation.
     * Solo carga los campos estrictamente necesarios para construir el principal.
     */
    public Usuario(Long id, String email, RolType rol) {
        this.id = id;
        this.email = email;
        this.rol = rol;
        this.estado = true; // Si llegamos aquí desde un JWT válido, el usuario está activo
    }

    // ─── UserDetails ────────────────────────────────────────────────────────────

    /** El "username" de Spring Security es el email del usuario. */
    @Override
    public String getUsername() {
        return email;
    }

    /** Retorna el rol como GrantedAuthority con prefijo ROLE_ (ej: ROLE_ADMIN). */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()));
    }

    // getPassword() — Spring Security la lee del campo password generado por Lombok

    @Override
    public boolean isEnabled() {
        return estado != null && estado;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}
