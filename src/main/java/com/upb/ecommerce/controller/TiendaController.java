package com.upb.ecommerce.controller;

import com.upb.ecommerce.dto.TiendaRequest;
import com.upb.ecommerce.dto.TiendaResponse;
import com.upb.ecommerce.service.TiendaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tiendas")
public class TiendaController {

    private final TiendaService tiendaService;

    public TiendaController(TiendaService tiendaService) {
        this.tiendaService = tiendaService;
    }

    // GET /api/tiendas
    @GetMapping
    public ResponseEntity<List<TiendaResponse>> listarTodas() {
        return ResponseEntity.ok(tiendaService.listarTodas());
    }

    // GET /api/tiendas/1
    @GetMapping("/{id}")
    public ResponseEntity<TiendaResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(tiendaService.obtenerPorId(id));
    }

    // GET /api/tiendas/slug/comercio1
    @GetMapping("/slug/{slug}")
    public ResponseEntity<TiendaResponse> obtenerPorSlug(@PathVariable String slug) {
        return ResponseEntity.ok(tiendaService.obtenerPorSlug(slug));
    }

    // POST /api/tiendas
    @PostMapping
    public ResponseEntity<TiendaResponse> crear(@Valid @RequestBody TiendaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tiendaService.crear(request));
    }

    // PUT /api/tiendas/1
    @PutMapping("/{id}")
    public ResponseEntity<TiendaResponse> actualizar(@PathVariable Long id,
                                                     @Valid @RequestBody TiendaRequest request) {
        return ResponseEntity.ok(tiendaService.actualizar(id, request));
    }

    // DELETE /api/tiendas/1
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        tiendaService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
