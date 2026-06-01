package com.upb.ecommerce.core.integracion;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos del cliente que paga, anidados en el request de create-charge de Stereum.
 * name/lastname/document_number son requeridos para cobros en BOB; email/phone opcionales.
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class StereumCustomer {

    private String name;
    private String lastname;

    @JsonProperty("document_number")
    private String documentNumber;

    private String email;
    private String phone;
}
