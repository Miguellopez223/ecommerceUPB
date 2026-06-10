package com.upb.ecommerce.api.controller;

import com.upb.ecommerce.core.dto.response.CatalogoResponse;
import com.upb.ecommerce.core.service.CatalogoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Catálogo público de una tienda. Endpoint <b>público</b> (sin JWT): cualquiera puede ver
 * los productos de una tienda por su slug para el cierre conversacional por WhatsApp.
 */
@RestController
@RequestMapping("/api/catalogo")
public class CatalogoController {

    private final CatalogoService catalogoService;

    public CatalogoController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    @GetMapping("/{slug}")
    public ResponseEntity<CatalogoResponse> porSlug(@PathVariable String slug) {
        return ResponseEntity.ok(catalogoService.porSlug(slug));
    }
}
