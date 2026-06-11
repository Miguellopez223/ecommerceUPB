package com.upb.ecommerce.api.controller;

import com.upb.ecommerce.core.service.ProductoAsyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// --- PREGUNTA 5 ---
@RestController
@RequestMapping("/api/async/productos")
public class AsyncProductoController {

    private final ProductoAsyncService productoAsyncService;

    public AsyncProductoController(ProductoAsyncService productoAsyncService) {
        this.productoAsyncService = productoAsyncService;
    }

    // --- PREGUNTA 5 ---
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{productoId}/stock")
    public ResponseEntity<Map<String, String>> ajustarStock(@PathVariable Long productoId,
                                                            @RequestParam int nuevoStock) {
        // --- PREGUNTA 5 ---
        productoAsyncService.actualizarStockAsync(productoId, nuevoStock);

        return ResponseEntity.accepted().body(Map.of(
                "mensaje", "Ajuste de stock encolado (asíncrono). Revisar los logs para ver el resultado.",
                "productoId", String.valueOf(productoId),
                "nuevoStock", String.valueOf(nuevoStock)));
    }
}
