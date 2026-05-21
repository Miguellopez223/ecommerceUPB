package com.upb.ecommerce.api.controller;

import com.upb.ecommerce.core.dto.request.AtributoProductoRequest;
import com.upb.ecommerce.core.dto.response.AtributoProductoResponse;
import com.upb.ecommerce.core.service.AtributoProductoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/atributos")
public class AtributoProductoController {

    private final AtributoProductoService atributoService;

    public AtributoProductoController(AtributoProductoService atributoService) {
        this.atributoService = atributoService;
    }

    @GetMapping("/producto/{productoId}")
    public ResponseEntity<List<AtributoProductoResponse>> listarPorProducto(@PathVariable Long productoId) {
        return ResponseEntity.ok(atributoService.listarPorProducto(productoId));
    }

    @PostMapping
    public ResponseEntity<AtributoProductoResponse> agregar(@Valid @RequestBody AtributoProductoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(atributoService.agregar(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AtributoProductoResponse> actualizar(@PathVariable Long id,
                                                               @Valid @RequestBody AtributoProductoRequest request) {
        return ResponseEntity.ok(atributoService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        atributoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
