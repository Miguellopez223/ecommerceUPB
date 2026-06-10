package com.upb.ecommerce.data.repository;

import com.upb.ecommerce.domain.entities.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmailAndTiendaId(String email, Long tiendaId);

    /** Usuarios visibles como contacto en el catálogo público de la tienda. */
    List<Usuario> findByTiendaIdAndVisibleCatalogoTrueAndEstadoTrue(Long tiendaId);

    /** Usado por UserDetailsService — busca el primer usuario activo con ese email. */
    Optional<Usuario> findFirstByEmailAndEstadoTrue(String email);

    /**
     * Query optimizada para validar la sesión JWT en cada request.
     * Solo carga id, email y rol — evita el lazy-load de tienda y otros campos.
     * Construye una instancia parcial de Usuario vía el constructor (id, email, rol).
     */
    @Query("SELECT new com.upb.ecommerce.domain.entities.Usuario(u.id, u.email, u.rol) " +
           "FROM Usuario u WHERE u.id = :pId AND u.estado = true")
    Optional<Usuario> findByIdToValidateSession(@Param("pId") Long pId);
}
