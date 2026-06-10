package com.upb.ecommerce.data.repository;

import com.upb.ecommerce.domain.entities.UnidadMedida;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UnidadMedidaRepository extends JpaRepository<UnidadMedida, Long> {

    List<UnidadMedida> findByTiendaIdAndEstadoTrue(Long tiendaId);

    Optional<UnidadMedida> findByIdAndTiendaId(Long id, Long tiendaId);
}
