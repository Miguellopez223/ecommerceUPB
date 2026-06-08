package com.upb.ecommerce.core.integracion;

import com.upb.ecommerce.data.repository.PagoRepository;
import com.upb.ecommerce.data.repository.PedidoRepository;
import com.upb.ecommerce.domain.entities.Pago;
import com.upb.ecommerce.domain.entities.Pedido;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lógica de negocio para las notificaciones de webhook de Stereum (cambios de estado de
 * un cargo). El controlador ya validó la firma y el tiempo antes de llamar aquí.
 *
 * <p>Mapea el estado del cargo en Stereum al estado del {@link Pago} y del
 * {@link Pedido} de este sistema:
 * <ul>
 *   <li><b>PAGADO</b> → pago EXITOSO y pedido PAGADO.</li>
 *   <li><b>CANCELADO / ERROR</b> → pago RECHAZADO (el pedido sigue PENDIENTE para poder
 *       reintentar el cobro).</li>
 *   <li><b>PENDIENTE</b> → sin cambios (el cargo aún no se ha pagado).</li>
 * </ul>
 */
@Slf4j
@Service
public class StereumWebhookService {

    private final PagoRepository pagoRepository;
    private final PedidoRepository pedidoRepository;

    public StereumWebhookService(PagoRepository pagoRepository, PedidoRepository pedidoRepository) {
        this.pagoRepository = pagoRepository;
        this.pedidoRepository = pedidoRepository;
    }

    @Transactional
    public void procesarNotificacion(StereumWebhookNotificacion notificacion) {
        StereumWebhookTransaccion tx = notificacion.getTransaction();
        if (tx == null || tx.getId() == null) {
            log.warn("Notificación de transacción sin datos de transacción — se ignora");
            return;
        }

        Pago pago = pagoRepository.findByTransaccionPasarelaId(tx.getId()).orElse(null);
        if (pago == null) {
            // No conocemos esta transacción (p.ej. cobro generado fuera de este sistema).
            log.warn("No se encontró un pago para la transacción Stereum {} — se ignora", tx.getId());
            return;
        }

        // Idempotencia: si ya está cerrado, no reprocesamos (Stereum puede reenviar).
        if ("EXITOSO".equals(pago.getEstadoPago()) || "RECHAZADO".equals(pago.getEstadoPago())) {
            log.info("El pago {} ya está en estado {} — notificación ignorada",
                    pago.getId(), pago.getEstadoPago());
            return;
        }

        String estadoStereum = tx.getStatus();
        log.info("Procesando notificación Stereum: transacción {} → estado {}",
                tx.getId(), estadoStereum);

        switch (estadoStereum) {
            case "PAGADO" -> {
                pago.setEstadoPago("EXITOSO");
                pagoRepository.save(pago);

                Pedido pedido = pago.getPedido();
                pedido.setEstadoPedido("PAGADO");
                pedidoRepository.save(pedido);
                log.info("Pago {} marcado EXITOSO y pedido {} marcado PAGADO",
                        pago.getId(), pedido.getId());
            }
            case "CANCELADO", "ERROR" -> {
                pago.setEstadoPago("RECHAZADO");
                pagoRepository.save(pago);
                log.info("Pago {} marcado RECHAZADO (estado Stereum {})", pago.getId(), estadoStereum);
            }
            default -> log.info("Estado Stereum {} sin acción (pago {} sigue PENDIENTE)",
                    estadoStereum, pago.getId());
        }
    }
}
