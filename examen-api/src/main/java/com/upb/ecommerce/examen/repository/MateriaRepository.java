package com.upb.ecommerce.examen.repository;

import com.upb.ecommerce.examen.entity.Materia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MateriaRepository extends JpaRepository<Materia, Long> {
    Optional<Materia> findBySigla(String sigla);
}
