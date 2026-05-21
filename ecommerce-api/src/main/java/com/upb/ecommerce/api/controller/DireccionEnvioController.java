package com.upb.ecommerce.api.controller;

import com.upb.ecommerce.core.dto.request.DireccionEnvioRequest;
import com.upb.ecommerce.core.dto.response.DireccionEnvioResponse;
import com.upb.ecommerce.core.service.DireccionEnvioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/direcciones")
public class DireccionEnvioController {

    private final DireccionEnvioService direccionService;

    public DireccionEnvioController(DireccionEnvioService direccionService) {
        this.direccionService = direccionService;
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<DireccionEnvioResponse>> listarPorUsuario(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(direccionService.listarPorUsuario(usuarioId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DireccionEnvioResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(direccionService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<DireccionEnvioResponse> crear(@Valid @RequestBody DireccionEnvioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(direccionService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DireccionEnvioResponse> actualizar(@PathVariable Long id,
                                                             @Valid @RequestBody DireccionEnvioRequest request) {
        return ResponseEntity.ok(direccionService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        direccionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
