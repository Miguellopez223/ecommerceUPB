package com.upb.ecommerce.data.repository;

import com.upb.ecommerce.domain.entities.DetalleCarrito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DetalleCarritoRepository extends JpaRepository<DetalleCarrito, Long> {

    Optional<DetalleCarrito> findByCarritoIdAndProductoId(Long carritoId, Long productoId);
}
