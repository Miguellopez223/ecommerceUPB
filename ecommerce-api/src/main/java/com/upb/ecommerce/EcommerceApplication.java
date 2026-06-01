package com.upb.ecommerce;

import com.upb.ecommerce.core.integracion.SistemaExternoAuthRequest;
import com.upb.ecommerce.core.integracion.SistemaExternoService;
import com.upb.ecommerce.core.integracion.TiendaExternoResponse;
import com.upb.ecommerce.core.integracion.UsuarioExternoResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.List;

/**
 * Punto de entrada de la aplicacion.
 * @SpringBootApplication escanea com.upb.ecommerce.** lo que incluye
 * los beans de los modulos domain, data, core y api.
 *
 * <p>Implementa {@link CommandLineRunner} para consumir el backend del sistema externo
 * al arrancar (mismo patron que EventopApplication): se autentica y luego lista los
 * usuarios y las tiendas del otro proyecto. Las operaciones de crear ({@code crearUsuario},
 * {@code crearTienda}) quedan disponibles en {@link SistemaExternoService} para invocarlas
 * desde la logica de backend cuando se necesite.
 */
@Slf4j
@SpringBootApplication
public class EcommerceApplication implements CommandLineRunner {

    @Autowired
    private SistemaExternoService sistemaExternoService;

    public static void main(String[] args) {
        SpringApplication.run(EcommerceApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            // 1. Autenticarse contra el sistema externo (cachea el token)
            SistemaExternoAuthRequest authRequest = new SistemaExternoAuthRequest();
            authRequest.setEmail("admin@comercio1.com");
            authRequest.setPassword("123456");
            authRequest.setTiendaId(1L);
            sistemaExternoService.auth(authRequest);

            // 2. Consumir las APIs del sistema externo
            List<TiendaExternoResponse> tiendas = sistemaExternoService.listarTiendas();
            log.info("Tiendas del sistema externo: {}", tiendas);

            List<UsuarioExternoResponse> usuarios = sistemaExternoService.listarUsuarios(1L);
            log.info("Usuarios (tienda 1) del sistema externo: {}", usuarios);
        } catch (Exception e) {
            log.warn("No se pudo consumir el sistema externo al arrancar: {}", e.getMessage());
        }
    }
}
