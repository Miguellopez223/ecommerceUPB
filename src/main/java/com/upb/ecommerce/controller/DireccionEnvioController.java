package com.upb.ecommerce.controller;

import com.upb.ecommerce.dto.DireccionEnvioRequest;
import com.upb.ecommerce.dto.DireccionEnvioResponse;
import com.upb.ecommerce.service.DireccionEnvioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/direcciones")
public class DireccionEnvioController {

    private final DireccionEnvioService direccionEnvioService;

    public DireccionEnvioController(DireccionEnvioService direccionEnvioService) {
        this.direccionEnvioService = direccionEnvioService;
    }

    // GET /api/direcciones/usuario/1
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<DireccionEnvioResponse>> listarPorUsuario(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(direccionEnvioService.listarPorUsuario(usuarioId));
    }

    // GET /api/direcciones/1
    @GetMapping("/{id}")
    public ResponseEntity<DireccionEnvioResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(direccionEnvioService.obtenerPorId(id));
    }

    // POST /api/direcciones
    @PostMapping
    public ResponseEntity<DireccionEnvioResponse> crear(@Valid @RequestBody DireccionEnvioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(direccionEnvioService.crear(request));
    }

    // PUT /api/direcciones/1
    @PutMapping("/{id}")
    public ResponseEntity<DireccionEnvioResponse> actualizar(@PathVariable Long id,
                                                             @Valid @RequestBody DireccionEnvioRequest request) {
        return ResponseEntity.ok(direccionEnvioService.actualizar(id, request));
    }

    // DELETE /api/direcciones/1
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        direccionEnvioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
