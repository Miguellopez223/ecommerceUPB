package com.upb.ecommerce.repository;

import com.upb.ecommerce.repository.entities.Tienda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TiendaRepository extends JpaRepository<Tienda, Long> {

    // Busca una tienda por su URL amigable
    Optional<Tienda> findBySlug(String slug);
}
