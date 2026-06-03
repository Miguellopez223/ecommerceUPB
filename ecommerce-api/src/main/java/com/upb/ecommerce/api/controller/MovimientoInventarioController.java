package com.upb.ecommerce.api.controller;

import com.upb.ecommerce.core.dto.request.MovimientoInventarioRequest;
import com.upb.ecommerce.core.dto.response.MovimientoInventarioResponse;
import com.upb.ecommerce.core.service.MovimientoInventarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventario")
@PreAuthorize("hasRole('ADMIN')")
public class MovimientoInventarioController {

    private final MovimientoInventarioService movimientoService;

    public MovimientoInventarioController(MovimientoInventarioService movimientoService) {
        this.movimientoService = movimientoService;
    }

    @GetMapping("/tienda/{tiendaId}")
    public ResponseEntity<List<MovimientoInventarioResponse>> listarPorTienda(@PathVariable Long tiendaId) {
        return ResponseEntity.ok(movimientoService.listarPorTienda(tiendaId));
    }

    @GetMapping("/producto/{productoId}")
    public ResponseEntity<List<MovimientoInventarioResponse>> listarPorProducto(@PathVariable Long productoId) {
        return ResponseEntity.ok(movimientoService.listarPorProducto(productoId));
    }

    @PostMapping
    public ResponseEntity<MovimientoInventarioResponse> registrar(
            @Valid @RequestBody MovimientoInventarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(movimientoService.registrar(request));
    }
}
