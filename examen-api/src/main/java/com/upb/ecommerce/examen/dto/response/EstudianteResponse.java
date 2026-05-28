package com.upb.ecommerce.examen.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.upb.ecommerce.examen.entity.Estudiante;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class EstudianteResponse {

    private Long id;
    private String nombre;
    private String apellido;

    @JsonProperty("materia_id")
    private Long materiaId;

    @JsonProperty("materia_nombre")
    private String materiaNombre;

    private Integer nota;

    @JsonProperty("nro_telefono")
    private String nroTelefono;

    @JsonProperty("nro_documento")
    private String nroDocumento;

    public static EstudianteResponse fromEntity(Estudiante e) {
        return EstudianteResponse.builder()
                .id(e.getId())
                .nombre(e.getNombre())
                .apellido(e.getApellido())
                .materiaId(e.getMateria() != null ? e.getMateria().getId() : null)
                .materiaNombre(e.getMateria() != null ? e.getMateria().getNombre() : null)
                .nota(e.getNota())
                .nroTelefono(e.getNroTelefono())
                .nroDocumento(e.getNroDocumento())
                .build();
    }
}
