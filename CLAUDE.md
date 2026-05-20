# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Build
mvn clean install

# Run (starts on port 8081)
mvn spring-boot:run

# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=ClassName

# Run a single test method
mvn test -Dtest=ClassName#methodName

# Skip tests during build
mvn clean install -DskipTests
```

## Prerequisites

- Java 21
- PostgreSQL database `ecommerceUPB` on localhost:5432
- Database schema must already exist (Hibernate ddl-auto is set to `validate`)

## Architecture

**Stack:** Spring Boot 4.0.6, Spring Data JPA, Spring Security (not yet configured), PostgreSQL, Lombok.

**Multi-tenant design:** Every entity is scoped to a `Tienda` (store). Users, products, orders, carts, and categories all belong to a specific tienda. Email uniqueness for users is per-tienda, not global.

**Package layout** (`com.upb.ecommerce`):
- `repository/entities/` — JPA entities (the domain model)
- `repository/` — Spring Data JPA repositories
- `service/` — Business logic (empty, to be built)
- `controller/` — REST endpoints (empty, to be built)
- `seeders/DataSeeder` — Runs on startup via `CommandLineRunner`, seeds a default tienda and admin user if they don't exist

**Domain model — key entities and relationships:**
- `Tienda` → owns `Usuario`, `Producto`, `Categoria`, `Pedido`, `Carrito`, `MovimientoInventario`
- `Usuario` → has `Pedido`, `Carrito`, `DireccionEnvio`
- `Producto` → has `AtributoProducto` (color, size, etc.)
- `Carrito` → has `DetalleCarrito` line items; status: ACTIVO / CONVERTIDO_A_PEDIDO / ABANDONADO
- `Pedido` → has `DetallePedido` line items, `Pago`, linked `DireccionEnvio`; status: PENDIENTE → PAGADO → PREPARANDO → ENVIADO → ENTREGADO / CANCELADO

**Conventions:**
- All entity table/column names use Spanish (e.g., `tiendas`, `usuarios`, `pedidos`)
- Lombok `@Data` + `@NoArgsConstructor` on all entities
- FetchType.LAZY on `@ManyToOne` relationships
- Constructor-based dependency injection (no `@Autowired` on fields)
- User roles are string-based: `"ADMIN"` or `"CLIENTE"`
