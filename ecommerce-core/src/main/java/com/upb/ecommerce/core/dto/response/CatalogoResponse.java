package com.upb.ecommerce.core.dto.response;

import lombok.Data;

import java.util.List;

/**
 * Vista pública (sin login) del catálogo de una tienda, accesible por su slug.
 * Incluye los datos de la tienda, sus productos activos y los contactos de WhatsApp
 * habilitados para el cierre conversacional.
 */
@Data
public class CatalogoResponse {

    private TiendaResponse tienda;
    private List<ProductoResponse> productos;
    private List<String> contactosWhatsapp;
}
