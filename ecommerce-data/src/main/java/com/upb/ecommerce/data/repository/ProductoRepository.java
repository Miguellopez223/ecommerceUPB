package com.upb.ecommerce.data.repository;

import com.upb.ecommerce.domain.entities.Producto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    List<Producto> findByTiendaIdAndEstadoTrue(Long tiendaId);

    Page<Producto> findByTiendaIdAndEstadoTrue(Long tiendaId, Pageable pageable);

    List<Producto> findByTiendaIdAndCategoriaIdAndEstadoTrue(Long tiendaId, Long categoriaId);

    Optional<Producto> findByIdAndTiendaId(Long id, Long tiendaId);

    Optional<Producto> findBySlugProductoAndTiendaId(String slugProducto, Long tiendaId);

    long countByTiendaIdAndEstadoTrue(Long tiendaId);

    /** Productos agotados (stock = 0) y activos. */
    long countByTiendaIdAndEstadoTrueAndStock(Long tiendaId, Integer stock);

    /** Productos activos cuyo stock está en o por debajo del mínimo (incluye agotados). */
    @Query("SELECT p FROM Producto p WHERE p.tienda.id = :tiendaId AND p.estado = true " +
           "AND p.stock <= p.stockMinimo ORDER BY p.stock ASC")
    List<Producto> findStockCritico(@Param("tiendaId") Long tiendaId);
}
