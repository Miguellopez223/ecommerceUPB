package com.upb.ecommerce.examen.repository;

import com.upb.ecommerce.examen.entity.Estudiante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EstudianteRepository extends JpaRepository<Estudiante, Long> {
    Optional<Estudiante> findByNroDocumento(String nroDocumento);
    List<Estudiante> findByMateriaId(Long materiaId);
}
