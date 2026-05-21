package com.upb.ecommerce.repository;

import com.upb.ecommerce.repository.entities.DireccionEnvio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DireccionEnvioRepository extends JpaRepository<DireccionEnvio, Long> {

    List<DireccionEnvio> findByUsuarioIdAndEstadoTrue(Long usuarioId);
}
