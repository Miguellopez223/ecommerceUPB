package com.upb.ecommerce.controller;

import com.upb.ecommerce.dto.AgregarItemCarritoRequest;
import com.upb.ecommerce.dto.CarritoResponse;
import com.upb.ecommerce.service.CarritoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carrito")
public class CarritoController {

    private final CarritoService carritoService;

    public CarritoController(CarritoService carritoService) {
        this.carritoService = carritoService;
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
}
