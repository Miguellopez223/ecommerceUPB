package com.upb.ecommerce.core.service;

import com.upb.ecommerce.core.dto.request.PagoRequest;
import com.upb.ecommerce.core.dto.response.PagoResponse;
import com.upb.ecommerce.core.exception.NotDataFoundException;
import com.upb.ecommerce.core.exception.OperationException;
import com.upb.ecommerce.data.repository.PagoRepository;
import com.upb.ecommerce.data.repository.PedidoRepository;
import com.upb.ecommerce.domain.entities.Pago;
import com.upb.ecommerce.domain.entities.Pedido;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PagoService {

    private final PagoRepository pagoRepository;
    private final PedidoRepository pedidoRepository;

    public PagoService(PagoRepository pagoRepository, PedidoRepository pedidoRepository) {
        this.pagoRepository = pagoRepository;
        this.pedidoRepository = pedidoRepository;
    }

    public List<PagoResponse> listarPorPedido(Long pedidoId) {
        return pagoRepository.findByPedidoId(pedidoId)
                .stream().map(PagoResponse::fromEntity).toList();
    }

    @Transactional
    public PagoResponse registrar(PagoRequest request) {
        Pedido pedido = pedidoRepository.findById(request.getPedidoId())
                .orElseThrow(() -> new NotDataFoundException("Pedido no encontrado"));

        if (request.getMonto().compareTo(pedido.getTotal()) != 0) {
            throw new OperationException("El monto (" + request.getMonto()
                    + ") no coincide con el total del pedido (" + pedido.getTotal() + ")");
        }

        Pago pago = new Pago();
        pago.setPedido(pedido);
        pago.setMetodo(request.getMetodo());
        pago.setTransaccionPasarelaId(request.getTransaccionPasarelaId());
        pago.setMonto(request.getMonto());
        pago.setEstadoPago("EXITOSO");

        pedido.setEstadoPedido("PAGADO");
        pedidoRepository.save(pedido);

        return PagoResponse.fromEntity(pagoRepository.save(pago));
    }
}
