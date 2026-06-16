package com.upb.ecommerce.api.controller;

import com.upb.ecommerce.api.scheduler.CarritoAbandonadoJob;
import com.upb.ecommerce.core.dto.request.AgregarItemCarritoRequest;
import com.upb.ecommerce.core.dto.response.CarritoResponse;
import com.upb.ecommerce.core.service.CarritoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/carrito")
public class CarritoController {

    private final CarritoService carritoService;
    private final CarritoAbandonadoJob carritoAbandonadoJob;

    public CarritoController(CarritoService carritoService,
                             CarritoAbandonadoJob carritoAbandonadoJob) {
        this.carritoService = carritoService;
        this.carritoAbandonadoJob = carritoAbandonadoJob;
    }

    @GetMapping("/tienda/{tiendaId}/usuario/{usuarioId}")
    public ResponseEntity<CarritoResponse> obtenerCarrito(@PathVariable Long tiendaId,
                                                          @PathVariable Long usuarioId) {
        return ResponseEntity.ok(carritoService.obtenerCarritoActivo(tiendaId, usuarioId));
    }

    @PostMapping("/agregar")
    public ResponseEntity<CarritoResponse> agregarItem(@Valid @RequestBody AgregarItemCarritoRequest request) {
        return ResponseEntity.ok(carritoService.agregarItem(request));
    }

    @DeleteMapping("/{carritoId}/item/{detalleId}")
    public ResponseEntity<CarritoResponse> eliminarItem(@PathVariable Long carritoId,
                                                        @PathVariable Long detalleId) {
        return ResponseEntity.ok(carritoService.eliminarItem(carritoId, detalleId));
    }

    @DeleteMapping("/{carritoId}/vaciar")
    public ResponseEntity<CarritoResponse> vaciar(@PathVariable Long carritoId) {
        return ResponseEntity.ok(carritoService.vaciarCarrito(carritoId));
    }

    /**
     * Dispara manualmente el barrido de carritos abandonados (el mismo que corre cada 8h por
     * {@code @Scheduled}). Útil para demos: no hay que esperar al cron. Solo ADMIN.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/barrer-abandonados")
    public ResponseEntity<Map<String, String>> barrerAbandonados() {
        carritoAbandonadoJob.barrerCarritosAbandonados();
        return ResponseEntity.ok(Map.of("mensaje", "Barrido de carritos abandonados ejecutado. Revisa los logs."));
    }
}
