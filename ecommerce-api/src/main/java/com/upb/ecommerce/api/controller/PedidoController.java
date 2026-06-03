package com.upb.ecommerce.api.controller;

import com.upb.ecommerce.core.dto.request.CrearPedidoRequest;
import com.upb.ecommerce.core.dto.request.GenerarQrRequest;
import com.upb.ecommerce.core.dto.response.PedidoResponse;
import com.upb.ecommerce.core.integracion.StereumCreateChargeResponse;
import com.upb.ecommerce.core.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @GetMapping("/tienda/{tiendaId}/usuario/{usuarioId}")
    public ResponseEntity<List<PedidoResponse>> listarPorUsuario(@PathVariable Long tiendaId,
                                                                  @PathVariable Long usuarioId) {
        return ResponseEntity.ok(pedidoService.listarPorUsuario(tiendaId, usuarioId));
    }

    @GetMapping("/tienda/{tiendaId}/{pedidoId}")
    public ResponseEntity<PedidoResponse> obtenerPorId(@PathVariable Long tiendaId,
                                                       @PathVariable Long pedidoId) {
        return ResponseEntity.ok(pedidoService.obtenerPorId(tiendaId, pedidoId));
    }

    @PostMapping
    public ResponseEntity<PedidoResponse> crearDesdeCarrito(@Valid @RequestBody CrearPedidoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pedidoService.crearDesdeCarrito(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/tienda/{tiendaId}/{pedidoId}/estado")
    public ResponseEntity<PedidoResponse> actualizarEstado(@PathVariable Long tiendaId,
                                                           @PathVariable Long pedidoId,
                                                           @RequestParam String estado) {
        return ResponseEntity.ok(pedidoService.actualizarEstado(tiendaId, pedidoId, estado));
    }

    @PatchMapping("/tienda/{tiendaId}/{pedidoId}/cancelar")
    public ResponseEntity<PedidoResponse> cancelar(@PathVariable Long tiendaId,
                                                   @PathVariable Long pedidoId) {
        return ResponseEntity.ok(pedidoService.cancelarPedido(tiendaId, pedidoId));
    }

    /**
     * Genera el QR de pago (Stereum) para el pedido. El monto se toma del total
     * del pedido; el body es opcional (país, moneda, red, documento del pagador).
     */
    @PostMapping("/tienda/{tiendaId}/{pedidoId}/qr")
    public ResponseEntity<StereumCreateChargeResponse> generarQr(
            @PathVariable Long tiendaId,
            @PathVariable Long pedidoId,
            @RequestBody(required = false) GenerarQrRequest request) {
        try {
            GenerarQrRequest req = request != null ? request : new GenerarQrRequest();
            return ResponseEntity.ok(pedidoService.generarQrPago(tiendaId, pedidoId, req));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "No se pudo generar el QR de pago: " + e.getMessage());
        }
    }
}
