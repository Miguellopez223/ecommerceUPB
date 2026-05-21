package com.upb.ecommerce.data.seeders;

import com.upb.ecommerce.data.repository.TiendaRepository;
import com.upb.ecommerce.data.repository.UsuarioRepository;
import com.upb.ecommerce.domain.entities.Tienda;
import com.upb.ecommerce.domain.entities.Usuario;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final TiendaRepository tiendaRepository;
    private final UsuarioRepository usuarioRepository;

    public DataSeeder(TiendaRepository tiendaRepository, UsuarioRepository usuarioRepository) {
        this.tiendaRepository = tiendaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public void run(String... args) {
        System.out.println("==================================================");
        System.out.println("INICIANDO POBLACIÓN DE DATOS DE PRUEBA...");
        System.out.println("==================================================");

        Tienda tienda = tiendaRepository.findBySlug("comercio1")
                .orElseGet(() -> {
                    System.out.println("Creando tienda matriz: Comercio1...");
                    Tienda nueva = new Tienda();
                    nueva.setNombre("Comercio1 Inventario General");
                    nueva.setSlug("comercio1");
                    nueva.setTelefonoContacto("77712345");
                    nueva.setEmailContacto("contacto@comercio1.com");
                    return tiendaRepository.save(nueva);
                });

        usuarioRepository.findByEmailAndTiendaId("admin@comercio1.com", tienda.getId())
                .orElseGet(() -> {
                    System.out.println("Creando usuario administrador...");
                    Usuario admin = new Usuario();
                    admin.setTienda(tienda);
                    admin.setNombre("Roberto Rodriguez");
                    admin.setEmail("admin@comercio1.com");
                    admin.setPassword("123456"); // TODO: encriptar con BCrypt
                    admin.setRol("ADMIN");
                    return usuarioRepository.save(admin);
                });

        System.out.println("\nDATOS EN POSTGRESQL:");
        tiendaRepository.findAll().forEach(t ->
                System.out.println("  Tienda → ID: " + t.getId() + " | " + t.getNombre() + " | slug: " + t.getSlug()));
        usuarioRepository.findAll().forEach(u ->
                System.out.println("  Usuario → ID: " + u.getId() + " | " + u.getNombre() + " | Rol: " + u.getRol()));
        System.out.println("==================================================\n");
    }
}
