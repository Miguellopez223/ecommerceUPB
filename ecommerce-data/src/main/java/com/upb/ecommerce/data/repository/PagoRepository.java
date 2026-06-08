package com.upb.ecommerce.data.repository;

import com.upb.ecommerce.domain.entities.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {

    List<Pago> findByPedidoId(Long pedidoId);

    /** Busca el pago por el id de transacción que devolvió Stereum al crear el cargo. */
    Optional<Pago> findByTransaccionPasarelaId(String transaccionPasarelaId);
}
