package com.upb.ecommerce.controller;

import com.upb.ecommerce.dto.AtributoProductoRequest;
import com.upb.ecommerce.dto.AtributoProductoResponse;
import com.upb.ecommerce.service.AtributoProductoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/atributos")
public class AtributoProductoController {

    private final AtributoProductoService atributoProductoService;

    public AtributoProductoController(AtributoProductoService atributoProductoService) {
        this.atributoProductoService = atributoProductoService;
    }

    // GET /api/atributos/producto/1
    @GetMapping("/producto/{productoId}")
    public ResponseEntity<List<AtributoProductoResponse>> listarPorProducto(@PathVariable Long productoId) {
        return ResponseEntity.ok(atributoProductoService.listarPorProducto(productoId));
    }

    // POST /api/atributos
    @PostMapping
    public ResponseEntity<AtributoProductoResponse> agregar(@Valid @RequestBody AtributoProductoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(atributoProductoService.agregar(request));
    }

    // PUT /api/atributos/1
    @PutMapping("/{id}")
    public ResponseEntity<AtributoProductoResponse> actualizar(@PathVariable Long id,
                                                               @Valid @RequestBody AtributoProductoRequest request) {
        return ResponseEntity.ok(atributoProductoService.actualizar(id, request));
    }

    // DELETE /api/atributos/1
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        atributoProductoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
