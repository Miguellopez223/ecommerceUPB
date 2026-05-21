package com.upb.ecommerce.controller;

import com.upb.ecommerce.dto.MovimientoInventarioRequest;
import com.upb.ecommerce.dto.MovimientoInventarioResponse;
import com.upb.ecommerce.service.MovimientoInventarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventario")
public class MovimientoInventarioController {

    private final MovimientoInventarioService movimientoInventarioService;

    public MovimientoInventarioController(MovimientoInventarioService movimientoInventarioService) {
        this.movimientoInventarioService = movimientoInventarioService;
    }

    // GET /api/inventario/tienda/1
    @GetMapping("/tienda/{tiendaId}")
    public ResponseEntity<List<MovimientoInventarioResponse>> listarPorTienda(@PathVariable Long tiendaId) {
        return ResponseEntity.ok(movimientoInventarioService.listarPorTienda(tiendaId));
    }

    // GET /api/inventario/producto/1
    @GetMapping("/producto/{productoId}")
    public ResponseEntity<List<MovimientoInventarioResponse>> listarPorProducto(@PathVariable Long productoId) {
        return ResponseEntity.ok(movimientoInventarioService.listarPorProducto(productoId));
    }

    // POST /api/inventario
    @PostMapping
    public ResponseEntity<MovimientoInventarioResponse> registrar(
            @Valid @RequestBody MovimientoInventarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(movimientoInventarioService.registrar(request));
    }
}
