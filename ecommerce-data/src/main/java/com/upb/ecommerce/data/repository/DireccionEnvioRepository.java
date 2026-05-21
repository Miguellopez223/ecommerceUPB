package com.upb.ecommerce.data.repository;

import com.upb.ecommerce.domain.entities.DireccionEnvio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DireccionEnvioRepository extends JpaRepository<DireccionEnvio, Long> {

    List<DireccionEnvio> findByUsuarioIdAndEstadoTrue(Long usuarioId);
}
