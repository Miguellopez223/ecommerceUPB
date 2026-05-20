package com.upb.ecommerce.repository;

import com.upb.ecommerce.repository.entities.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Busca un usuario por email asegurando que pertenece a una tienda específica
    Optional<Usuario> findByEmailAndTiendaId(String email, Long tiendaId);
}
