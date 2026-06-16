package com.upb.ecommerce.data.repository;

import com.upb.ecommerce.domain.entities.Carrito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface CarritoRepository extends JpaRepository<Carrito, Long> {

    Optional<Carrito> findByUsuarioIdAndTiendaIdAndEstado(Long usuarioId, Long tiendaId, String estado);

    /**
     * Carritos en un estado dado cuya última modificación es anterior a {@code limite}.
     * Lo usa el barrido de carritos abandonados: estado = ACTIVO y fechaActualizacion vieja.
     */
    Page<Carrito> findByEstadoAndFechaActualizacionBefore(String estado, LocalDateTime limite, Pageable pageable);
}
