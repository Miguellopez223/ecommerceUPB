package com.upb.ecommerce.data.seeders;

import com.upb.ecommerce.data.repository.AtributoProductoRepository;
import com.upb.ecommerce.data.repository.CarritoRepository;
import com.upb.ecommerce.data.repository.CategoriaRepository;
import com.upb.ecommerce.data.repository.DetalleCarritoRepository;
import com.upb.ecommerce.data.repository.DireccionEnvioRepository;
import com.upb.ecommerce.data.repository.MovimientoInventarioRepository;
import com.upb.ecommerce.data.repository.PagoRepository;
import com.upb.ecommerce.data.repository.PedidoRepository;
import com.upb.ecommerce.data.repository.ProductoRepository;
import com.upb.ecommerce.data.repository.TiendaRepository;
import com.upb.ecommerce.data.repository.UsuarioRepository;
import com.upb.ecommerce.domain.entities.AtributoProducto;
import com.upb.ecommerce.domain.entities.Carrito;
import com.upb.ecommerce.domain.entities.Categoria;
import com.upb.ecommerce.domain.entities.DetalleCarrito;
import com.upb.ecommerce.domain.entities.DetallePedido;
import com.upb.ecommerce.domain.entities.DireccionEnvio;
import com.upb.ecommerce.domain.entities.MovimientoInventario;
import com.upb.ecommerce.domain.entities.Pago;
import com.upb.ecommerce.domain.entities.Pedido;
import com.upb.ecommerce.domain.entities.Producto;
import com.upb.ecommerce.domain.entities.Tienda;
import com.upb.ecommerce.domain.entities.Usuario;
import com.upb.ecommerce.domain.enums.RolType;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;

@Slf4j
@Component
@AllArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final TiendaRepository tiendaRepository;
    private final UsuarioRepository usuarioRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;
    private final AtributoProductoRepository atributoProductoRepository;
    private final DireccionEnvioRepository direccionEnvioRepository;
    private final CarritoRepository carritoRepository;
    private final DetalleCarritoRepository detalleCarritoRepository;
    private final PedidoRepository pedidoRepository;
    private final PagoRepository pagoRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        init();
    }

    private void init() {

        // ── 1. TIENDA ────────────────────────────────────────────────────────────
        Tienda tienda;
        if (tiendaRepository.count() == 0) {
            tienda = new Tienda();
            tienda.setNombre("Comercio1 Inventario General");
            tienda.setSlug("comercio1");
            tienda.setTelefonoContacto("77712345");
            tienda.setEmailContacto("contacto@comercio1.com");
            tiendaRepository.save(tienda);
            log.info(">> Tienda creada: {} (id={})", tienda.getNombre(), tienda.getId());
        } else {
            tienda = tiendaRepository.findBySlug("comercio1")
                    .orElseThrow(() -> new RuntimeException("Tienda inicial no encontrada"));
        }

        // ── 2. USUARIO ADMIN ─────────────────────────────────────────────────────
        Usuario admin;
        if (usuarioRepository.findByEmailAndTiendaId("admin@comercio1.com", tienda.getId()).isEmpty()) {
            admin = new Usuario();
            admin.setTienda(tienda);
            admin.setNombre("Roberto Rodriguez");
            admin.setEmail("admin@comercio1.com");
            admin.setPassword(passwordEncoder.encode("Admin123**"));
            admin.setRol(RolType.ADMIN);
            usuarioRepository.save(admin);
            log.info(">> Admin creado: {} | password: Admin123**", admin.getEmail());
        } else {
            admin = usuarioRepository.findByEmailAndTiendaId("admin@comercio1.com", tienda.getId()).get();
        }

        // ── 3. USUARIO CLIENTE ───────────────────────────────────────────────────
        Usuario cliente;
        if (usuarioRepository.findByEmailAndTiendaId("cliente@comercio1.com", tienda.getId()).isEmpty()) {
            cliente = new Usuario();
            cliente.setTienda(tienda);
            cliente.setNombre("Ana García");
            cliente.setEmail("cliente@comercio1.com");
            cliente.setPassword(passwordEncoder.encode("Cliente123**"));
            cliente.setRol(RolType.CLIENTE);
            usuarioRepository.save(cliente);
            log.info(">> Cliente creado: {} | password: Cliente123**", cliente.getEmail());
        } else {
            cliente = usuarioRepository.findByEmailAndTiendaId("cliente@comercio1.com", tienda.getId()).get();
        }

        // ── 4. CATEGORÍA ─────────────────────────────────────────────────────────
        Categoria categoria;
        if (categoriaRepository.count() == 0) {
            categoria = new Categoria();
            categoria.setTienda(tienda);
            categoria.setNombre("Electrónica");
            categoriaRepository.save(categoria);
            log.info(">> Categoría creada: {} (id={})", categoria.getNombre(), categoria.getId());
        } else {
            categoria = categoriaRepository.findAll().get(0);
        }

        // ── 5. PRODUCTO ──────────────────────────────────────────────────────────
        Producto producto;
        if (productoRepository.count() == 0) {
            producto = new Producto();
            producto.setTienda(tienda);
            producto.setCategoria(categoria);
            producto.setNombre("Smartphone Galaxy X10");
            producto.setSlugProducto("smartphone-galaxy-x10");
            producto.setDescripcionLarga("Smartphone de última generación con pantalla AMOLED 6.7\" y batería de 5000 mAh.");
            producto.setPrecio(new BigDecimal("1599.99"));
            producto.setPrecioCosto(new BigDecimal("1100.00"));
            producto.setStock(50);
            producto.setStockMinimo(5);
            producto.setImagenUrl("https://example.com/img/galaxy-x10.jpg");
            productoRepository.save(producto);
            log.info(">> Producto creado: {} (id={})", producto.getNombre(), producto.getId());
        } else {
            producto = productoRepository.findAll().get(0);
        }

        // ── 6. ATRIBUTO DE PRODUCTO ──────────────────────────────────────────────
        if (atributoProductoRepository.count() == 0) {
            AtributoProducto atributo = new AtributoProducto();
            atributo.setProducto(producto);
            atributo.setNombre("Color");
            atributo.setValor("Negro Medianoche");
            atributoProductoRepository.save(atributo);
            log.info(">> Atributo creado: {}={}", atributo.getNombre(), atributo.getValor());
        }

        // ── 7. DIRECCIÓN DE ENVÍO ────────────────────────────────────────────────
        DireccionEnvio direccion;
        if (direccionEnvioRepository.count() == 0) {
            direccion = new DireccionEnvio();
            direccion.setUsuario(cliente);
            direccion.setDireccionCalle("Av. Ballivián #1234, Zona Sur");
            direccion.setCiudad("Cochabamba");
            direccion.setReferencias("Edificio Torre Azul, piso 3");
            direccionEnvioRepository.save(direccion);
            log.info(">> Dirección creada: {} (id={})", direccion.getDireccionCalle(), direccion.getId());
        } else {
            direccion = direccionEnvioRepository.findAll().get(0);
        }

        // ── 8. CARRITO ACTIVO + DETALLE ──────────────────────────────────────────
        Carrito carrito;
        if (carritoRepository.count() == 0) {
            carrito = new Carrito();
            carrito.setTienda(tienda);
            carrito.setUsuario(cliente);
            carrito.setDetalles(new ArrayList<>());
            carritoRepository.save(carrito);

            DetalleCarrito detalle = new DetalleCarrito();
            detalle.setCarrito(carrito);
            detalle.setProducto(producto);
            detalle.setCantidad(2);
            detalle.setPrecioUnitario(producto.getPrecio());
            detalleCarritoRepository.save(detalle);

            // Calcular total
            carrito.setTotalEstimado(producto.getPrecio().multiply(new BigDecimal("2")));
            carritoRepository.save(carrito);
            log.info(">> Carrito ACTIVO creado (id={}) con 2x {}", carrito.getId(), producto.getNombre());
        } else {
            carrito = carritoRepository.findAll().get(0);
        }

        // ── 9. PEDIDO + DETALLE ──────────────────────────────────────────────────
        Pedido pedido;
        if (pedidoRepository.count() == 0) {
            pedido = new Pedido();
            pedido.setTienda(tienda);
            pedido.setUsuario(cliente);
            pedido.setDireccionEnvio(direccion);
            pedido.setCodigoSeguimiento("PED-SEED0001");
            pedido.setEstadoPedido("PENDIENTE");
            pedido.setDetalles(new ArrayList<>());

            DetallePedido detallePedido = new DetallePedido();
            detallePedido.setPedido(pedido);
            detallePedido.setProducto(producto);
            detallePedido.setCantidad(1);
            detallePedido.setPrecioUnitario(producto.getPrecio());
            pedido.getDetalles().add(detallePedido);

            pedido.setTotal(producto.getPrecio());
            pedidoRepository.save(pedido);

            // Descontar stock
            producto.setStock(producto.getStock() - 1);
            productoRepository.save(producto);

            log.info(">> Pedido creado: {} (id={})", pedido.getCodigoSeguimiento(), pedido.getId());
        } else {
            pedido = pedidoRepository.findAll().get(0);
        }

        // ── 10. PAGO ─────────────────────────────────────────────────────────────
        if (pagoRepository.count() == 0) {
            Pago pago = new Pago();
            pago.setPedido(pedido);
            pago.setMetodo("QR");
            pago.setTransaccionPasarelaId("TXN-QR-20250101-0001");
            pago.setMonto(pedido.getTotal());
            pago.setEstadoPago("PENDIENTE");
            pagoRepository.save(pago);
            log.info(">> Pago creado: metodo={} monto={} (id={})", pago.getMetodo(), pago.getMonto(), pago.getId());
        }

        // ── 11. MOVIMIENTO DE INVENTARIO ─────────────────────────────────────────
        if (movimientoInventarioRepository.count() == 0) {
            MovimientoInventario movimiento = new MovimientoInventario();
            movimiento.setTienda(tienda);
            movimiento.setProducto(producto);
            movimiento.setUsuario(admin);
            movimiento.setTipo("ENTRADA");
            movimiento.setCantidad(50);
            movimiento.setReferencia("Carga inicial de inventario — seeder");
            movimientoInventarioRepository.save(movimiento);
            log.info(">> Movimiento ENTRADA creado: {} unidades de {}",
                    movimiento.getCantidad(), producto.getNombre());
        }

        log.info("=== DataSeeder finalizado. Base de datos lista para pruebas. ===");
    }
}
