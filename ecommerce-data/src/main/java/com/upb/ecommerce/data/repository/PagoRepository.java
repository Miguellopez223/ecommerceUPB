package com.upb.ecommerce.data.repository;

import com.upb.ecommerce.domain.entities.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {

    List<Pago> findByPedidoId(Long pedidoId);

    /** Busca el pago por el id de transacción que devolvió Stereum al crear el cargo. */
    Optional<Pago> findByTransaccionPasarelaId(String transaccionPasarelaId);

    /** Pagos exitosos de la tienda en un rango (para el desglose por método de pago). */
    @Query("SELECT pg FROM Pago pg JOIN pg.pedido p WHERE p.tienda.id = :tiendaId " +
           "AND pg.estadoPago = 'EXITOSO' " +
           "AND p.fechaCreacion >= :desde AND p.fechaCreacion <= :hasta")
    List<Pago> findExitososEntre(@Param("tiendaId") Long tiendaId,
                                 @Param("desde") LocalDateTime desde,
                                 @Param("hasta") LocalDateTime hasta);
}
