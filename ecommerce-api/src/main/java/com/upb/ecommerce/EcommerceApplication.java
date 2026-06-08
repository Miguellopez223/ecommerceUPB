package com.upb.ecommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de la aplicacion.
 *
 * <p>{@code @SpringBootApplication} escanea {@code com.upb.ecommerce.**}, lo que incluye
 * los beans de los modulos domain, data, core y api.
 *
 * <p>La integracion con el sistema externo y con Stereum se expone como endpoints REST en
 * {@code IntegracionController} ({@code /api/integracion/**}) y {@code PedidoController}
 * ({@code /api/pedidos/.../qr}); no se ejecuta nada de eso al arrancar.
 */
@SpringBootApplication
public class EcommerceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EcommerceApplication.class, args);
    }
}
