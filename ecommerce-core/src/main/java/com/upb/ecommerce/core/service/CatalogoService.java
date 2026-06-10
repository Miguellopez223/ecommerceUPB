package com.upb.ecommerce.core.service;

import com.upb.ecommerce.core.dto.response.CatalogoResponse;
import com.upb.ecommerce.core.dto.response.ProductoResponse;
import com.upb.ecommerce.core.dto.response.TiendaResponse;
import com.upb.ecommerce.core.exception.NotDataFoundException;
import com.upb.ecommerce.data.repository.ProductoRepository;
import com.upb.ecommerce.data.repository.TiendaRepository;
import com.upb.ecommerce.data.repository.UsuarioRepository;
import com.upb.ecommerce.domain.entities.Tienda;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Catálogo público de una tienda (sin autenticación), resuelto por el slug de la tienda.
 */
@Service
public class CatalogoService {

    private final TiendaRepository tiendaRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;

    public CatalogoService(TiendaRepository tiendaRepository,
                           ProductoRepository productoRepository,
                           UsuarioRepository usuarioRepository) {
        this.tiendaRepository = tiendaRepository;
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public CatalogoResponse porSlug(String slug) {
        Tienda tienda = tiendaRepository.findBySlug(slug)
                .orElseThrow(() -> new NotDataFoundException("Tienda no encontrada: " + slug));

        CatalogoResponse dto = new CatalogoResponse();
        dto.setTienda(TiendaResponse.fromEntity(tienda));
        dto.setProductos(productoRepository.findByTiendaIdAndEstadoTrue(tienda.getId())
                .stream().map(ProductoResponse::fromEntity).toList());
        dto.setContactosWhatsapp(
                usuarioRepository.findByTiendaIdAndVisibleCatalogoTrueAndEstadoTrue(tienda.getId())
                        .stream()
                        .map(u -> u.getNumeroWhatsapp())
                        .filter(n -> n != null && !n.isBlank())
                        .toList());
        return dto;
    }
}
