package com.upb.ecommerce.examen.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class EstudianteRequest {

    @NotBlank
    private String nombre;

    @NotBlank
    private String apellido;

    @NotNull
    @JsonProperty("materia_id")
    private Long materiaId;

    @NotNull
    @Min(0)
    @Max(100)
    private Integer nota;

    @JsonProperty("nro_telefono")
    private String nroTelefono;

    @NotBlank
    @JsonProperty("nro_documento")
    private String nroDocumento;
}
