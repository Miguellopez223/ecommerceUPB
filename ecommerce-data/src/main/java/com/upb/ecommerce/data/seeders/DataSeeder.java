package com.upb.ecommerce.data.seeders;

import com.upb.ecommerce.data.repository.TiendaRepository;
import com.upb.ecommerce.data.repository.UsuarioRepository;
import com.upb.ecommerce.domain.entities.Tienda;
import com.upb.ecommerce.domain.entities.Usuario;
import com.upb.ecommerce.domain.enums.RolType;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final TiendaRepository tiendaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        init();
    }

    private void init() {
        // Crear tienda inicial solo si no existe ninguna
        if (tiendaRepository.count() == 0) {
            Tienda tienda = new Tienda();
            tienda.setNombre("Comercio1 Inventario General");
            tienda.setSlug("comercio1");
            tienda.setTelefonoContacto("77712345");
            tienda.setEmailContacto("contacto@comercio1.com");
            tiendaRepository.save(tienda);
            log.info("Tienda inicial creada: {}", tienda.getNombre());
        }

        // Crear usuario admin solo si no existe ningún usuario
        if (usuarioRepository.count() == 0) {
            Tienda tienda = tiendaRepository.findBySlug("comercio1")
                    .orElseThrow(() -> new RuntimeException("Tienda inicial no encontrada"));

            Usuario admin = new Usuario();
            admin.setTienda(tienda);
            admin.setNombre("Roberto Rodriguez");
            admin.setEmail("admin@comercio1.com");
            admin.setPassword(passwordEncoder.encode("Admin123**"));
            admin.setRol(RolType.ADMIN);
            usuarioRepository.save(admin);
            log.info("Usuario admin creado: {} | password: Admin123**", admin.getEmail());
        }
    }
}
