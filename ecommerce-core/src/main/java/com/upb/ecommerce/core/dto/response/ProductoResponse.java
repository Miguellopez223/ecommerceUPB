package com.upb.ecommerce.core.dto.response;

import com.upb.ecommerce.domain.entities.Producto;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductoResponse {

    private Long id;
    private Long tiendaId;
    private Long categoriaId;
    private String categoriaNombre;
    private String nombre;
    private String slugProducto;
    private String descripcionLarga;
    private BigDecimal precio;
    private BigDecimal precioCosto;
    private Integer stock;
    private Integer stockMinimo;
    private String imagenUrl;
    private Boolean estado;

    public static ProductoResponse fromEntity(Producto p) {
        ProductoResponse r = new ProductoResponse();
        r.setId(p.getId());
        r.setTiendaId(p.getTienda().getId());
        if (p.getCategoria() != null) {
            r.setCategoriaId(p.getCategoria().getId());
            r.setCategoriaNombre(p.getCategoria().getNombre());
        }
        r.setNombre(p.getNombre());
        r.setSlugProducto(p.getSlugProducto());
        r.setDescripcionLarga(p.getDescripcionLarga());
        r.setPrecio(p.getPrecio());
        r.setPrecioCosto(p.getPrecioCosto());
        r.setStock(p.getStock());
        r.setStockMinimo(p.getStockMinimo());
        r.setImagenUrl(p.getImagenUrl());
        r.setEstado(p.getEstado());
        return r;
    }
}
