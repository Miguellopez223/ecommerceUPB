package com.upb.ecommerce.core.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductoRequest {

    @NotNull
    private Long tiendaId;

    private Long categoriaId;

    private Long unidadMedidaId;

    @NotBlank
    private String nombre;

    @NotBlank
    private String slugProducto;

    private String descripcionLarga;

    @NotNull
    @Positive
    private BigDecimal precio;

    private BigDecimal precioCosto;

    @NotNull
    private Integer stock;

    private Integer stockMinimo;

    private String imagenUrl;
}
