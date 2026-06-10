package com.upb.ecommerce.data.repository;

import com.upb.ecommerce.domain.entities.MovimientoInventario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {

    List<MovimientoInventario> findByProductoIdOrderByFechaDesc(Long productoId);

    List<MovimientoInventario> findByTiendaIdOrderByFechaDesc(Long tiendaId);

    long countByTiendaId(Long tiendaId);

    /**
     * Última fecha de SALIDA por producto (para detectar productos "muertos").
     * Devuelve filas [productoId (Long), ultimaFecha (LocalDateTime)].
     */
    @Query("SELECT m.producto.id, MAX(m.fecha) FROM MovimientoInventario m " +
           "WHERE m.tienda.id = :tiendaId AND m.tipo = 'SALIDA' GROUP BY m.producto.id")
    List<Object[]> ultimaSalidaPorProducto(@Param("tiendaId") Long tiendaId);

    /**
     * Movimientos filtrados por tipo / usuario / rango de fechas (cualquiera opcional).
     * Usado por la pestaña "Movimientos" de reportes.
     */
    @Query("SELECT m FROM MovimientoInventario m WHERE m.tienda.id = :tiendaId " +
           "AND (:tipo IS NULL OR m.tipo = :tipo) " +
           "AND (:usuarioId IS NULL OR m.usuario.id = :usuarioId) " +
           "AND (:desde IS NULL OR m.fecha >= :desde) " +
           "AND (:hasta IS NULL OR m.fecha <= :hasta) " +
           "ORDER BY m.fecha DESC")
    List<MovimientoInventario> filtrar(@Param("tiendaId") Long tiendaId,
                                       @Param("tipo") String tipo,
                                       @Param("usuarioId") Long usuarioId,
                                       @Param("desde") LocalDateTime desde,
                                       @Param("hasta") LocalDateTime hasta);
}
