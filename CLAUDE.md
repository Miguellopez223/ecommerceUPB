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

**Stack:** Spring Boot 4.0.6, Spring Data JPA, Spring Security + JWT, PostgreSQL, Lombok.

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
- `com.upb.ecommerce.domain.enums` — RolType, UsuarioStatus
- `com.upb.ecommerce.data.repository` — repositories
- `com.upb.ecommerce.data.seeders` — DataSeeder
- `com.upb.ecommerce.core.service` — services
- `com.upb.ecommerce.core.dto.request` / `.response` — DTOs
- `com.upb.ecommerce.core.exception` — custom exceptions (NotDataFoundException)
- `com.upb.ecommerce.api.controller` — controllers (including AuthController)
- `com.upb.ecommerce.api.config` — SecurityConfig, JwtTokenProvider, JwtTokenFilter, GlobalExceptionHandler
- `com.upb.ecommerce.api.exception` — InvalidJwtAuthenticationException

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
- User roles use `RolType` enum: `ADMIN` or `CLIENTE` (stored as String via `@Enumerated(EnumType.STRING)`)
- Services throw `RuntimeException` with Spanish messages for not-found cases

**Security:**
- JWT-based stateless authentication (token expires after 480 minutes / 8 hours)
- JWT claims: `sub` = email, `jti` = userId (used for session validation via optimized JPQL query)
- `Usuario` entity implements `UserDetails` — used directly as the Spring Security principal
- Password encoding: `DelegatingPasswordEncoder` (bcrypt default) via `InjectConfiguration`
- Public endpoints: `POST /api/auth` (login), `POST /api/usuarios/registrar`, `GET/POST /api/tiendas`, Swagger UI
- All other endpoints require a valid JWT in the `Authorization: Bearer <token>` header
- Login requires email + password + tiendaId (multi-tenant auth)

**Error handling (GlobalExceptionHandler):**
- `NotDataFoundException` → 404
- `RuntimeException` → 400 (used as general business-logic error)
- `MethodArgumentNotValidException` → 400 with field-level errors map
- `InvalidJwtAuthenticationException` → 401
