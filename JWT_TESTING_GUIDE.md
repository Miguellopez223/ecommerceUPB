# JWT + Autenticación — Guía de Pruebas

## Credenciales de Prueba (DataSeeder)

El proyecto crea automáticamente al iniciar:

```
Email:    admin@comercio1.com
Password: 123456
TiendaId: 1
Rol:      ADMIN
```

## Paso 1: Obtener el JWT

**POST** `http://localhost:8081/api/usuarios/login`

**Body (JSON):**
```json
{
  "email": "admin@comercio1.com",
  "password": "123456",
  "tiendaId": 1
}
```

**Respuesta exitosa (200):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "bearer",
  "expiresIn": 28800000,
  "expiresAt": 1653465600000
}
```

## Paso 2: Usar el Token en Requests Protegidos

Para cualquier endpoint protegido, agrega el header:

```
Authorization: Bearer <accessToken>
```

**Ejemplo con cURL:**
```bash
curl -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  http://localhost:8081/api/categorias/tienda/1
```

**Ejemplo con Postman:**
1. Copia el `accessToken` del response anterior
2. Ve a la pestaña **Headers**
3. Agrega: `Authorization: Bearer <paste-token-here>`
4. Envía el request

## Endpoints Públicos (Sin Token)

```
POST   /api/usuarios/registrar
POST   /api/usuarios/login
GET    /api/tiendas
POST   /api/tiendas
GET    /swagger-ui.html
```

## Endpoints Protegidos (Requieren JWT)

```
GET    /api/usuarios/tienda/{tiendaId}
GET    /api/usuarios/{id}
PUT    /api/usuarios/{id}
DELETE /api/usuarios/{id}
GET    /api/categorias/tienda/{tiendaId}
POST   /api/categorias
PUT    /api/categorias/{id}
DELETE /api/categorias/{id}
... (todos los demás)
```

## Ciclo de Vida del Token

- **Duración:** 480 minutos = 8 horas (configurable en `application.properties`)
- **Renovación:** POST `/api/usuarios/login` con credenciales válidas para obtener un nuevo token
- **Expiración:** Si expires, recibirás **401 Unauthorized**

## Troubleshooting

### ❌ "401 Unauthorized" sin token en header protegido
→ Asegúrate de enviar `Authorization: Bearer <token>`

### ❌ "401 Unauthorized" con token expirado
→ Haz login nuevamente para obtener un nuevo token

### ❌ "401 Unauthorized" con token inválido
→ Verifica que el token sea el correcto (sin espacios extra)

### ❌ "400 Bad Request" en login
→ Verifica credenciales:
  - Email correcto
  - Password correcta
  - TiendaId existe

## Cambiar Contraseña del Admin

Si necesitas cambiar la contraseña seeded, haz:

**PUT** `http://localhost:8081/api/usuarios/1`

**Header:**
```
Authorization: Bearer <token>
```

**Body:**
```json
{
  "tiendaId": 1,
  "nombre": "Roberto Rodriguez",
  "email": "admin@comercio1.com",
  "password": "nueva_contraseña",
  "rol": "ADMIN"
}
```

## Notas de Seguridad

⚠️ **Para desarrollo solamente:**
- La clave JWT en `application.properties` es publica (cambiar en producción)
- CORS permite `*` (cualquier origen) — restringir en producción
- Las contraseñas se hashean con BCrypt

## Verificar Detalles del Token

Si quieres ver el contenido del token (sin validar firma), usa:
https://jwt.io/

Pega el token en **Encoded** → verás el payload decodificado.
