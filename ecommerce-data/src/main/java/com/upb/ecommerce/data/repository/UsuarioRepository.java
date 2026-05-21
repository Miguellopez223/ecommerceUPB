package com.upb.ecommerce.data.repository;

import com.upb.ecommerce.domain.entities.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmailAndTiendaId(String email, Long tiendaId);
}
