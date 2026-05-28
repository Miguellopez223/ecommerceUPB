package com.upb.ecommerce.examen.dto.response;

import com.upb.ecommerce.examen.entity.Materia;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class MateriaResponse {

    private Long id;
    private String nombre;
    private String sigla;

    public static MateriaResponse fromEntity(Materia m) {
        return MateriaResponse.builder()
                .id(m.getId())
                .nombre(m.getNombre())
                .sigla(m.getSigla())
                .build();
    }
}
