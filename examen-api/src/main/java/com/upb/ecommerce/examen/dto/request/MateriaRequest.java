package com.upb.ecommerce.examen.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class MateriaRequest {

    @NotBlank
    private String nombre;

    @NotBlank
    private String sigla;
}
