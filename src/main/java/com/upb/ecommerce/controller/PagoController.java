package com.upb.ecommerce.controller;

import com.upb.ecommerce.dto.PagoRequest;
import com.upb.ecommerce.dto.PagoResponse;
import com.upb.ecommerce.service.PagoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    private final PagoService pagoService;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    // GET /api/pagos/pedido/1
    @GetMapping("/pedido/{pedidoId}")
    public ResponseEntity<List<PagoResponse>> listarPorPedido(@PathVariable Long pedidoId) {
        return ResponseEntity.ok(pagoService.listarPorPedido(pedidoId));
    }

    // POST /api/pagos
    @PostMapping
    public ResponseEntity<PagoResponse> registrar(@Valid @RequestBody PagoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pagoService.registrar(request));
    }
}
