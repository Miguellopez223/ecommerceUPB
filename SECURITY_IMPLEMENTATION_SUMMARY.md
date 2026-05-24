# Resumen: Implementación JWT + Seguridad

## ¿Qué se implementó?

Se aplicó el patrón JWT (JSON Web Tokens) con BCrypt del proyecto de ejemplo del docente (**eventop**) al proyecto ecommerce, respetando la arquitectura multi-módulo y el flujo de negocio multi-tienda.

---

## Archivos Nuevos Creados

### En `ecommerce-api/src/main/java/com/upb/ecommerce/api/config/`

| Archivo | Propósito |
|---------|-----------|
| **CorsFilter.java** | Maneja CORS. Permite requests desde cualquier origen con headers flexibles. |
| **InjectConfiguration.java** | Define el bean `PasswordEncoder` (BCrypt delegado). |
| **UsuarioPrincipal.java** | Adaptador que envuelve `Usuario` y lo expone como `UserDetails`. Mantiene la entidad de dominio limpia. |
| **JwtTokenProvider.java** | Crea tokens JWT con claims (email, userId, expiración). Valida tokens en cada request. |
| **JwtTokenFilter.java** | Filtro de servlet que intercepta requests, extrae JWT del header `Authorization: Bearer ...` y autentica. |
| **SecurityConfig.java** | Configuración de Spring Security: define qué endpoints son públicos, cuáles protegidos, y cadena de filtros. |

### En `ecommerce-api/src/main/java/com/upb/ecommerce/api/exception/`

| Archivo | Propósito |
|---------|-----------|
| **InvalidJwtAuthenticationException.java** | Excepción lanzada en validación JWT fallida (→ 401). |

### En `ecommerce-core/src/main/java/com/upb/ecommerce/core/dto/response/`

| Archivo | Propósito |
|---------|-----------|
| **LoginResponse.java** | DTO que retorna el token JWT en `/login`: `accessToken`, `tokenType`, `expiresIn`, `expiresAt`. |

---

## Archivos Modificados

### Dependencias

| Módulo | Cambio |
|--------|--------|
| **ecommerce-api/pom.xml** | + `spring-boot-starter-security` + jjwt (api, impl, jackson) versión 0.12.6 |
| **ecommerce-core/pom.xml** | + `spring-security-core` (scope: provided) |
| **ecommerce-data/pom.xml** | + `spring-security-core` (scope: provided) |

### Lógica de Negocio

| Archivo | Cambio |
|---------|--------|
| **UsuarioService.java** | ✅ `registrar()` → BCrypt real con PasswordEncoder<br/>✅ `validarCredenciales(LoginRequest)` → nuevo, para login<br/>✅ `findByIdForSession(Long)` → nuevo, para JwtTokenProvider<br/>✅ Inyección de PasswordEncoder |
| **UsuarioController.java** | ✅ `POST /login` ahora devuelve `LoginResponse` con JWT (no `UsuarioResponse`)<br/>✅ Inyección de JwtTokenProvider |
| **DataSeeder.java** | ✅ Password del admin se guarda con BCrypt (via PasswordEncoder) |
| **GlobalExceptionHandler.java** | ✅ Handler para `InvalidJwtAuthenticationException` → 401 |
| **application.properties** | ✅ `security.jwt.token.secret-key` (Base64)<br/>✅ `security.jwt.token.expire-length` (minutos) |

---

## Flujo de Autenticación

```
1. Cliente hace POST /api/usuarios/login
   ↓
2. UsuarioController.login() valida credenciales
   → UsuarioService.validarCredenciales() → Usuario
   ↓
3. JwtTokenProvider.createToken(usuario) → LoginResponse con JWT
   ↓
4. Cliente recibe { accessToken, tokenType, expiresIn, expiresAt }
   ↓
5. Cliente incluye en próximos requests:
   Header: Authorization: Bearer <accessToken>
   ↓
6. JwtTokenFilter intercepta y valida el token
   → JwtTokenProvider.validateToken()
   ↓
7. Si válido, autentica al usuario (UsuarioPrincipal en SecurityContext)
   Si expirado/inválido, devuelve 401
```

---

## Configuración de Seguridad (Endpoints)

### Públicos (sin JWT)
```
POST   /api/usuarios/registrar    — crear usuario
POST   /api/usuarios/login        — obtener JWT
GET    /api/tiendas              — listar tiendas
POST   /api/tiendas              — crear tienda
GET    /swagger-ui/**            — documentación
```

### Protegidos (requieren JWT)
```
GET    /api/usuarios/tienda/{tiendaId}
GET    /api/usuarios/{id}
PUT    /api/usuarios/{id}
DELETE /api/usuarios/{id}
GET    /api/categorias/**        — y todos los demás
POST   /api/categorias/**
PUT    /api/categorias/**
... (todos excepto los públicos)
```

---

## Diferencias vs. Proyecto Eventop

| Aspecto | Eventop | Ecommerce |
|--------|---------|-----------|
| **ID Usuario** | String (UUID) | Long (IDENTITY) |
| **Email** | Único globalmente | Único por tienda |
| **Búsqueda en JWT** | `findByUserIdToValidateSession(String)` | `findByIdForSession(Long)` |
| **Estructura** | Monolítica | Multi-módulo Maven |
| **DTOs** | En misma capa | Separados en core |
| **AuditorAware** | Implementado | Solo bean, sin auditoría en entidades |

---

## Testing

Ver `JWT_TESTING_GUIDE.md` para:
- Credenciales de prueba
- Cómo obtener el JWT
- Cómo usarlo en Postman
- Troubleshooting

---

## Notas Importantes

⚠️ **Antes de iniciar:**
1. PostgreSQL debe estar corriendo
2. BD `ecommerceUPB` debe existir (con schema válido)
3. Las contraseñas seeded NO se pueden cambiar manualmente en BD (están hashadas)

⚠️ **Primer login:**
- Email: `admin@comercio1.com`
- Password: `123456`
- El DataSeeder crea esto automáticamente al iniciar

⚠️ **Seguridad (desarrollo):**
- La clave JWT está en propiedades (cambiar en producción)
- CORS es `*` (solo desarrollo)
- Passwords son BCrypt (✅ seguro)

---

## Cambios Futuros Opcionales

- [ ] Refresh tokens (para renovar sin re-login)
- [ ] Rate limiting (prevenir fuerza bruta)
- [ ] Auditoría (quién modificó qué cuándo)
- [ ] Logout (invalidar tokens activos)
- [ ] OAuth2 con proveedores externos
- [ ] 2FA (autenticación de dos factores)
