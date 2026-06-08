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
- PostgreSQL database `ecommerceUPB` on localhost:5432 (user: `postgres`, password: `password`)
- Database schema must already exist (Hibernate ddl-auto is set to `validate`)
- DataSeeder auto-creates an admin user on startup: `admin@comercio1.com` / `123456`

## Getting Started

1. Ensure PostgreSQL is running and the `ecommerceUPB` database exists
2. Run `mvn clean install` from the root directory to build all modules
3. Start the app with `mvn -pl ecommerce-api spring-boot:run` (runs on `http://localhost:8081`)
4. Access Swagger UI at `http://localhost:8081/swagger-ui.html` to explore and test endpoints
5. Login with admin credentials to get a JWT token; include it as `Authorization: Bearer <token>` in subsequent requests

## Architecture

**Stack:** Spring Boot 4.0.6, Spring Data JPA, Spring Security + JWT (jjwt 0.12.6), PostgreSQL, Lombok.

**Multi-module Maven project** (dependency flows top-down):

```
ecommerce-domain   — JPA entities + enums (no Spring dependencies beyond JPA annotations)
       ↓
ecommerce-data     — Spring Data JPA repositories + DataSeeder (CommandLineRunner)
       ↓
ecommerce-core     — Services + DTOs (request/response) + NotDataFoundException. Bean Validation on request DTOs.
       ↓
ecommerce-api      — REST controllers, JWT auth (JwtTokenProvider, JwtTokenFilter), SecurityConfig, GlobalExceptionHandler, app entry point
```

**Package namespaces per module:**
- `com.upb.ecommerce.domain.entities` — entities
- `com.upb.ecommerce.domain.enums` — `RolType` (ADMIN, CLIENTE), `UsuarioStatus`
- `com.upb.ecommerce.data.repository` — repositories
- `com.upb.ecommerce.data.seeders` — DataSeeder
- `com.upb.ecommerce.core.service` — services (includes `UsuarioDetailsServiceImpl` for Spring Security)
- `com.upb.ecommerce.core.dto.request` / `.response` — DTOs
- `com.upb.ecommerce.core.exception` — `NotDataFoundException`
- `com.upb.ecommerce.core.integracion` — DTOs + `SistemaExternoService` for consuming a remote ecommerce instance via RestClient
- `com.upb.ecommerce.api.controller` — controllers (includes `AuthController` at `/api/auth`)
- `com.upb.ecommerce.api.config` — SecurityConfig, JwtTokenProvider, JwtTokenFilter, CorsFilter, GlobalExceptionHandler
- `com.upb.ecommerce.api.exception` — `InvalidJwtAuthenticationException`

**Legacy single-module code** still exists under `src/main/java/com/upb/ecommerce/`. The active code is in the four `ecommerce-*` modules; the `src/` tree is not part of the build.

## Authentication & Security

JWT-based stateless authentication. Login flow:
1. `POST /api/auth` with `{email, password, tiendaId}` → returns `LoginResponse` with JWT
2. Subsequent requests include `Authorization: Bearer <token>`
3. `JwtTokenFilter` validates the token and sets `UsuarioPrincipal` in SecurityContext

**Logout:** `POST /api/auth/logout` adds the current token to `TokenBlacklist` (an in-memory denylist keyed by token → expiration) until it would naturally expire — needed because JWTs are otherwise stateless and remain cryptographically valid until expiry. `JwtTokenFilter` rejects any request whose token is blacklisted.

**Public endpoints (no JWT required):**
- `POST /api/auth` — login
- `POST /api/auth/externo` — authenticate against the external system
- `POST /api/usuarios/registrar` — register
- `GET/POST /api/tiendas` — list/create stores
- `/swagger-ui/**`, `/v3/api-docs/**` — API docs

All other endpoints require a valid JWT.

## Multi-tenant Design

Every entity is scoped to a `Tienda` (store). Users, products, orders, carts, and categories all belong to a specific tienda. Email uniqueness for users is per-tienda, not global. Login requires `tiendaId` in the request.

## Domain Model — Key Entities

- `Tienda` → owns `Usuario`, `Producto`, `Categoria`, `Pedido`, `Carrito`, `MovimientoInventario`
- `Usuario` → has `Pedido`, `Carrito`, `DireccionEnvio`
- `Producto` → has `AtributoProducto` (color, size, etc.)
- `Carrito` → has `DetalleCarrito` line items; status: ACTIVO / CONVERTIDO_A_PEDIDO / ABANDONADO
- `Pedido` → has `DetallePedido` line items, `Pago`, linked `DireccionEnvio`; status: PENDIENTE → PAGADO → PREPARANDO → ENVIADO → ENTREGADO / CANCELADO

## External System Integration

### Remote Ecommerce Instance
`SistemaExternoService` in `ecommerce-core` consumes a peer ecommerce instance (configured via `sistema.externo.url` in application.properties). Flow: authenticate first via `POST /api/auth/externo`, which caches the JWT token in-memory, then call endpoints to list/create clients and products on the remote system. Uses Spring's `RestClient`.

### Stereum API (QR Payment Generation)
QR code generation for payments is handled via Stereum API (`stereum.url-base` and `stereum.api-key` in application.properties). The integration generates payment QR codes for orders. See relevant endpoints under `Pedido` resource for QR generation details. Stereum API calls use RestClient with configurable timeouts (read-timeout is high at 20s to allow QR generation).

## Conventions

- All entity table/column names use Spanish (e.g., `tiendas`, `usuarios`, `pedidos`)
- Lombok `@Data` + `@NoArgsConstructor` on all entities
- FetchType.LAZY on `@ManyToOne` relationships
- Constructor-based dependency injection (no `@Autowired` on fields)
- Soft-delete pattern (set `estado = false`) instead of hard deletes
- REST endpoints under `/api/<resource>`. Controllers use `@Valid @RequestBody` for input
- Response DTOs have a static `fromEntity()` factory method for entity→DTO mapping
- Services throw `NotDataFoundException` with Spanish messages for not-found cases
- User roles are the `RolType` enum (`ADMIN`, `CLIENTE`)
- Passwords stored with BCrypt (via `PasswordEncoder` bean in `InjectConfiguration`)

## Common Gotchas

- **Legacy code under `src/`**: The multi-module code under `ecommerce-*` is active. The old single-module code tree at `src/main/java/com/upb/ecommerce/` is legacy and not part of the build — avoid editing it.
- **Database schema validation**: Hibernate ddl-auto is set to `validate`, so the schema must exist before startup. If adding new entities, manually create the table first or temporarily switch to `update` mode.
- **Per-tienda email uniqueness**: User email addresses are unique per tienda, not globally. Different stores can have users with the same email.
- **JWT expiration**: Token expiration is set to 480 minutes (8 hours) in application.properties. Tokens will silently fail after expiration; clients must re-login.
- **Lazy loading outside transactions**: If you access lazy-loaded relationships outside a transaction (e.g., in a controller after the service returns), you'll get a LazyInitializationException. Use `@Transactional` on service methods or eagerly fetch in queries.
