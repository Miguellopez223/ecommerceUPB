package com.upb.ecommerce.core.dto.request;

import lombok.Data;

/**
 * Parámetros opcionales para generar el QR de pago (Stereum) de un pedido.
 * El monto NO se incluye: se toma siempre del total del pedido para evitar
 * manipulación. Todos los campos tienen valores por defecto sensatos para
 * cobros en Bolivia (BOB / CSL).
 */
@Data
public class GenerarQrRequest {

    /** Código ISO del país. Default: "BO". */
    private String country = "BO";

    /** Moneda del cobro: "BOB", "USDT", "USDC". Default: "BOB". */
    private String currency = "BOB";

    /** Red o banco recaudador: "CSL" (BOB) o "POLYGON" (USDT/USDC). Default: "CSL". */
    private String network = "CSL";

    /** Documento de identidad del pagador (requerido por Stereum para BOB). */
    private String documentNumber;

    /** Validez del QR en minutos. Default: "10". */
    private String reservationValidityTime = "10";
}
