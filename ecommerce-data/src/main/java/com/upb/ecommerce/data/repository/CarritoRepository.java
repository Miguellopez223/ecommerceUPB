package com.upb.ecommerce.data.repository;

import com.upb.ecommerce.domain.entities.Carrito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CarritoRepository extends JpaRepository<Carrito, Long> {

    Optional<Carrito> findByUsuarioIdAndTiendaIdAndEstado(Long usuarioId, Long tiendaId, String estado);
}
