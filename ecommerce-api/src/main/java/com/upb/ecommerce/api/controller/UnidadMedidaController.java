package com.upb.ecommerce.api.controller;

import com.upb.ecommerce.core.dto.request.UnidadMedidaRequest;
import com.upb.ecommerce.core.dto.response.UnidadMedidaResponse;
import com.upb.ecommerce.core.service.UnidadMedidaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/unidades-medida")
public class UnidadMedidaController {

    private final UnidadMedidaService unidadMedidaService;

    public UnidadMedidaController(UnidadMedidaService unidadMedidaService) {
        this.unidadMedidaService = unidadMedidaService;
    }

    @GetMapping("/tienda/{tiendaId}")
    public ResponseEntity<List<UnidadMedidaResponse>> listarPorTienda(@PathVariable Long tiendaId) {
        return ResponseEntity.ok(unidadMedidaService.listarPorTienda(tiendaId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<UnidadMedidaResponse> crear(@Valid @RequestBody UnidadMedidaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(unidadMedidaService.crear(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<UnidadMedidaResponse> actualizar(@PathVariable Long id,
                                                           @Valid @RequestBody UnidadMedidaRequest request) {
        return ResponseEntity.ok(unidadMedidaService.actualizar(id, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/tienda/{tiendaId}/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long tiendaId, @PathVariable Long id) {
        unidadMedidaService.eliminar(tiendaId, id);
        return ResponseEntity.noContent().build();
    }
}
