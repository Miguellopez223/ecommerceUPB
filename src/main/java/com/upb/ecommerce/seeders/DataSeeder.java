package com.upb.ecommerce.seeders;

import com.upb.ecommerce.repository.TiendaRepository;
import com.upb.ecommerce.repository.UsuarioRepository;
import com.upb.ecommerce.repository.entities.Tienda;
import com.upb.ecommerce.repository.entities.Usuario;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final TiendaRepository tiendaRepository;
    private final UsuarioRepository usuarioRepository;

    // Inyección de dependencias por constructor (Buena práctica en Spring)
    public DataSeeder(TiendaRepository tiendaRepository, UsuarioRepository usuarioRepository) {
        this.tiendaRepository = tiendaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("==================================================");
        System.out.println("INICIANDO POBLACIÓN DE DATOS DE PRUEBA...");
        System.out.println("==================================================");

        // 1. Verificar si ya existe la tienda para no duplicarla en cada reinicio
        Tienda tienda = tiendaRepository.findBySlug("comercio1")
                .orElseGet(() -> {
                    System.out.println("Creando tienda matriz: Comercio1...");
                    Tienda nuevaTienda = new Tienda();
                    nuevaTienda.setNombre("Comercio1 Inventario General");
                    nuevaTienda.setSlug("comercio1");
                    nuevaTienda.setTelefonoContacto("77712345");
                    nuevaTienda.setEmailContacto("contacto@comercio1.com");
                    return tiendaRepository.save(nuevaTienda);
                });

        // 2. Verificar si ya existe el usuario administrador
        usuarioRepository.findByEmailAndTiendaId("admin@comercio1.com", tienda.getId())
                .orElseGet(() -> {
                    System.out.println("➡Creando usuario administrador...");
                    Usuario nuevoAdmin = new Usuario();
                    nuevoAdmin.setTienda(tienda);
                    nuevoAdmin.setNombre("Roberto Rodriguez");
                    nuevoAdmin.setEmail("admin@comercio1.com");
                    nuevoAdmin.setPassword("123456"); // En un futuro esto irá encriptado con BCrypt
                    nuevoAdmin.setRol("ADMIN");
                    return usuarioRepository.save(nuevoAdmin);
                });

        // 3. MOSTRAR EN CONSOLA LO QUE HAY EN LA BASE DE DATOS
        System.out.println("\nDATOS GUARDADOS EN POSTGRESQL:");
        System.out.println("--------------------------------------------------");
        System.out.println("TIENDAS REGISTRADAS:");
        tiendaRepository.findAll().forEach(t -> {
            System.out.println(" - ID: " + t.getId() + " | Nombre: " + t.getNombre() + " | Slug: " + t.getSlug());
        });

        System.out.println("\nUSUARIOS REGISTRADOS:");
        usuarioRepository.findAll().forEach(u -> {
            System.out.println(" - ID: " + u.getId() + " | Nombre: " + u.getNombre() + " | Rol: " + u.getRol() + " | Tienda ID: " + u.getTienda().getId());
        });
        System.out.println("==================================================\n");
    }
}