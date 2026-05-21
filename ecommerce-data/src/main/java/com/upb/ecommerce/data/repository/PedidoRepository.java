package com.upb.ecommerce.data.repository;

import com.upb.ecommerce.domain.entities.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findByUsuarioIdAndTiendaId(Long usuarioId, Long tiendaId);

    Optional<Pedido> findByIdAndTiendaId(Long id, Long tiendaId);
}
