package com.upb.ecommerce.data.repository;

import com.upb.ecommerce.domain.entities.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    List<Producto> findByTiendaIdAndEstadoTrue(Long tiendaId);

    List<Producto> findByTiendaIdAndCategoriaIdAndEstadoTrue(Long tiendaId, Long categoriaId);

    Optional<Producto> findByIdAndTiendaId(Long id, Long tiendaId);

    Optional<Producto> findBySlugProductoAndTiendaId(String slugProducto, Long tiendaId);
}
