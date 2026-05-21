package com.upb.ecommerce.data.repository;

import com.upb.ecommerce.domain.entities.Tienda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TiendaRepository extends JpaRepository<Tienda, Long> {

    Optional<Tienda> findBySlug(String slug);
}
