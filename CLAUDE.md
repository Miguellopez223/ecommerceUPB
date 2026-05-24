# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Build all modules (from root)
mvn clean install

# Run the app (starts on port 8081) — must run from ecommerce-api
mvn -pl ecommerce-api spring-boot:run

# Run all tests
mvn test

# Run a single test class
mvn -pl ecommerce-api test -Dtest=ClassName

# Run a single test method
mvn -pl ecommerce-api test -Dtest=ClassName#methodName

# Skip tests during build
mvn clean install -DskipTests
```

## Prerequisites

- Java 21
- PostgreSQL database `ecommerceUPB` on localhost:5432
- Database schema must already exist (Hibernate ddl-auto is set to `validate`)

## Architecture

**Stack:** Spring Boot 4.0.6, Spring Data JPA, Spring Security (disabled — all endpoints permitAll), PostgreSQL, Lombok.

**Multi-module Maven project** (dependency flows top-down):

```
ecommerce-domain   — JPA entities (@Entity classes, no Spring dependencies beyond JPA annotations)
       ↓
ecommerce-data     — Spring Data JPA repositories + DataSeeder (CommandLineRunner)
       ↓
ecommerce-core     — Services + DTOs (request/response). Bean Validation on request DTOs.
       ↓
ecommerce-api      — REST controllers, Spring Security config, GlobalExceptionHandler, app entry point
```

**Package namespaces per module:**
- `com.upb.ecommerce.domain.entities` — entities
- `com.upb.ecommerce.data.repository` — repositories
- `com.upb.ecommerce.data.seeders` — DataSeeder
- `com.upb.ecommerce.core.service` — services
- `com.upb.ecommerce.core.dto.request` / `.response` — DTOs
- `com.upb.ecommerce.api.controller` — controllers
- `com.upb.ecommerce.api.config` — SecurityConfig, GlobalExceptionHandler

**Legacy single-module code** still exists under `src/main/java/com/upb/ecommerce/` (controllers, services, DTOs, repositories, entities). The active code is in the four `ecommerce-*` modules; the `src/` tree is from before the split and is not part of the build.

**Multi-tenant design:** Every entity is scoped to a `Tienda` (store). Users, products, orders, carts, and categories all belong to a specific tienda. Email uniqueness for users is per-tienda, not global.

**Domain model — key entities and relationships:**
- `Tienda` → owns `Usuario`, `Producto`, `Categoria`, `Pedido`, `Carrito`, `MovimientoInventario`
- `Usuario` → has `Pedido`, `Carrito`, `DireccionEnvio`
- `Producto` → has `AtributoProducto` (color, size, etc.)
- `Carrito` → has `DetalleCarrito` line items; status: ACTIVO / CONVERTIDO_A_PEDIDO / ABANDONADO
- `Pedido` → has `DetallePedido` line items, `Pago`, linked `DireccionEnvio`; status: PENDIENTE → PAGADO → PREPARANDO → ENVIADO → ENTREGADO / CANCELADO

**REST API pattern:** All endpoints under `/api/<resource>`. Controllers use `@Valid @RequestBody` for input. Soft-delete pattern (set `estado = false`) instead of hard deletes. Response DTOs have a static `fromEntity()` factory method for entity→DTO mapping.

**Conventions:**
- All entity table/column names use Spanish (e.g., `tiendas`, `usuarios`, `pedidos`)
- Lombok `@Data` + `@NoArgsConstructor` on all entities
- FetchType.LAZY on `@ManyToOne` relationships
- Constructor-based dependency injection (no `@Autowired` on fields)
- User roles are string-based: `"ADMIN"` or `"CLIENTE"`
- Services throw `RuntimeException` with Spanish messages for not-found cases
