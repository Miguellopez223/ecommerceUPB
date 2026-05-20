package com.upb.ecommerce.repository;

import com.upb.ecommerce.repository.entities.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findByTiendaIdAndEstadoTrue(Long tiendaId);
}
