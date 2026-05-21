package com.upb.ecommerce.api.controller;

import com.upb.ecommerce.core.dto.request.TiendaRequest;
import com.upb.ecommerce.core.dto.response.TiendaResponse;
import com.upb.ecommerce.core.service.TiendaService;
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

    @GetMapping
    public ResponseEntity<List<TiendaResponse>> listarTodas() {
        return ResponseEntity.ok(tiendaService.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TiendaResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(tiendaService.obtenerPorId(id));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<TiendaResponse> obtenerPorSlug(@PathVariable String slug) {
        return ResponseEntity.ok(tiendaService.obtenerPorSlug(slug));
    }

    @PostMapping
    public ResponseEntity<TiendaResponse> crear(@Valid @RequestBody TiendaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tiendaService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TiendaResponse> actualizar(@PathVariable Long id,
                                                     @Valid @RequestBody TiendaRequest request) {
        return ResponseEntity.ok(tiendaService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        tiendaService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
