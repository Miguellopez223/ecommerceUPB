package com.upb.ecommerce.api.controller;

import com.upb.ecommerce.core.dto.request.CrearPedidoRequest;
import com.upb.ecommerce.core.dto.response.PedidoResponse;
import com.upb.ecommerce.core.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PatchMapping("/tienda/{tiendaId}/{pedidoId}/estado")
    public ResponseEntity<PedidoResponse> actualizarEstado(@PathVariable Long tiendaId,
                                                           @PathVariable Long pedidoId,
                                                           @RequestParam String estado) {
        return ResponseEntity.ok(pedidoService.actualizarEstado(tiendaId, pedidoId, estado));
    }
}
