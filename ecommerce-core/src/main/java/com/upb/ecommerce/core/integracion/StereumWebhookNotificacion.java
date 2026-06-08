package com.upb.ecommerce.core.integracion;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Cuerpo (JSON) que Stereum Pay envía por POST al webhook cada vez que cambia el estado
 * de un cargo, o cuando valida la URL del webhook.
 *
 * <p>{@code notificationType} distingue el tipo de aviso:
 * <ul>
 *   <li><b>"test"</b> — notificación de prueba que Stereum manda al validar la URL del
 *       webhook en la consola; no lleva transacción y solo se debe responder 200.</li>
 *   <li><b>"transaction"</b> — cambio de estado real de un cargo; el detalle viene en
 *       {@link #transaction}.</li>
 * </ul>
 *
 * <p>{@code @JsonIgnoreProperties(ignoreUnknown = true)} hace que, si Stereum agrega
 * campos nuevos en el futuro, el parseo no falle (simplemente los ignora).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class StereumWebhookNotificacion {

    @JsonProperty("notification_type")
    private String notificationType;

    /** Identificador de la notificación (distinto del id de la transacción). */
    private String id;

    /** Detalle del cargo. Es null en las notificaciones de tipo "test". */
    private StereumWebhookTransaccion transaction;

    /** Momento de envío de la notificación, en milisegundos epoch. */
    private Long timestamp;
}
