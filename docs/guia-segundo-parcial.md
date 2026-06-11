# Guía de estudio — Spring Boot (2º parcial)

> Guía anclada en el código real del proyecto **ecommerce** (Spring Boot 4, multi-módulo).
> Cubre teoría + práctica + cómo se ve en este proyecto, con énfasis en:
> **HMAC & JWT · JSONObject · Stereum · Webhook · Transactions & métodos asíncronos · Logs · Seguridad · Jobs**.

> ⚠️ **Aviso de honestidad técnica:** dos temas del temario (**`JSONObject`** y **Jobs/async con `@Scheduled`/`@Async`**) **no se usan tal cual en este proyecto** — el JSON se resuelve con *Jackson* (no con `org.json.JSONObject`) y no hay `@Scheduled`/`@Async`. Se explica igual la teoría de ambos Y cómo lo hace realmente el proyecto, marcando la diferencia.

---

## 0. Qué es Spring Boot y cómo está armado este proyecto

**Spring** es un framework de inyección de dependencias (IoC: *Inversion of Control*). En vez de que vos crees los objetos con `new`, los crea y conecta el **contenedor de Spring**. **Spring Boot** es Spring + autoconfiguración + servidor embebido (Tomcat) + convenciones, para arrancar rápido.

**Conceptos núcleo que tenés que dominar:**

- **Bean**: un objeto gestionado por Spring. Se declara con `@Component`, `@Service`, `@Repository`, `@RestController`, `@Configuration` + `@Bean`.
- **Inyección de dependencias (DI)**: este proyecto usa **inyección por constructor** (la forma recomendada):
  ```java
  // PedidoController.java
  private final PedidoService pedidoService;
  public PedidoController(PedidoService pedidoService) {
      this.pedidoService = pedidoService;   // Spring pasa el bean automáticamente
  }
  ```
- **Estereotipos / capas** (arquitectura de este proyecto, 4 módulos Maven):
  - `domain` → entidades JPA (`@Entity`) y enums.
  - `data` → repositorios (`@Repository`, Spring Data JPA).
  - `core` → servicios (`@Service`) + DTOs + lógica de negocio.
  - `api` → controladores (`@RestController`) + configuración + seguridad.
  - El flujo de dependencias es **api → core → data → domain** (cada capa solo conoce la de abajo).
- **`@SpringBootApplication`**: en `EcommerceApplication.java`. Escanea `com.upb.ecommerce.**` y registra todos los beans de los 4 módulos.

> 💡 **Pregunta típica de examen:** "¿Por qué inyección por constructor y no por campo (`@Autowired` en el atributo)?" Respuesta: permite campos `final` (inmutables), facilita los tests (pasás mocks por el constructor) y deja explícitas las dependencias obligatorias.

---

## 1. Seguridad (Spring Security)

Spring Security es una **cadena de filtros** (`SecurityFilterChain`) que se ejecuta **antes** de tus controladores. Cada request pasa por esos filtros y, si no está autorizado, nunca llega al `@RestController`.

En `SecurityConfig.java` se define la política:

```java
http
  .csrf(AbstractHttpConfigurer::disable)          // 1
  .authorizeHttpRequests(auth -> auth
      .requestMatchers(HttpMethod.POST, "/api/auth").permitAll()   // 2 públicos
      .requestMatchers("/api/dashboard/**").authenticated()        // (vía anyRequest)
      .anyRequest().authenticated())                               // 3 todo lo demás
  .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))      // 4
  .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class); // 5
```

Puntos clave para el examen:

1. **CSRF deshabilitado**: CSRF protege formularios con sesión de cookie. Como esta API es **stateless con JWT** (no usa cookies de sesión), CSRF no aplica y se desactiva.
2. **`permitAll()` vs `authenticated()`**: endpoints públicos (login, registro, catálogo, webhook) vs los que exigen token.
3. **`anyRequest().authenticated()`**: regla "atrapa-todo" — lo que no esté explícitamente permitido, requiere autenticación.
4. **`STATELESS`**: el servidor **no guarda sesión**. Cada request se autentica solo con su JWT. Esto es central: es lo que hace escalable a la API, pero también obliga a la *blacklist* para el logout (ver §2).
5. **`addFilterBefore`**: insertamos nuestro `JwtTokenFilter` antes del filtro estándar de usuario/contraseña.

**Dos niveles de autorización** (te lo pueden preguntar):
- **A nivel de URL** (la config de arriba).
- **A nivel de método**, con `@EnableMethodSecurity` + `@PreAuthorize`:
  ```java
  @PreAuthorize("hasRole('ADMIN')")   // exige rol ADMIN
  @PostMapping public ... crear(...) { ... }
  ```
  Spring construye la autoridad como `ROLE_ADMIN` (prefijo `ROLE_`). Por eso en `Usuario.getAuthorities()`:
  ```java
  return List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()));
  ```

**Contraseñas**: nunca se guardan en texto plano. Se usa **BCrypt** (un hash con *salt* y costo configurable):
```java
usuario.setPassword(passwordEncoder.encode(request.getPassword())); // al registrar
passwordEncoder.matches(raw, hashGuardado);                          // al validar
```
BCrypt es **de un solo sentido**: no se "desencripta", solo se compara.

---

## 2. HMAC & JWT (tema fuerte)

Este es el tema más importante porque **JWT y HMAC están conectados**: la firma de un JWT (algoritmo HS256) *es* un HMAC-SHA256. Entender uno te explica el otro.

### 2.1 HMAC — teoría

**HMAC** = *Hash-based Message Authentication Code*. Es un código que prueba dos cosas a la vez:
- **Integridad**: el mensaje no fue alterado.
- **Autenticidad**: quien lo generó conoce una **clave secreta** compartida.

Fórmula conceptual: `HMAC = hash(clave_secreta + mensaje)`. La diferencia con un hash normal (SHA-256 a secas) es la **clave secreta**: sin ella no podés generar ni falsificar la firma. Es **simétrico** (la misma clave firma y verifica), a diferencia de la firma asimétrica (RSA, clave pública/privada).

### 2.2 HMAC en el proyecto — verificación del webhook de Stereum

Stereum nos manda notificaciones pero **no envía JWT**. ¿Cómo confiamos en que la petición es de verdad de Stereum? Con HMAC: Stereum firma el cuerpo con la **misma API Key** que nosotros tenemos, y manda la firma en el header `x-signature`. Nosotros **recalculamos** la firma y comparamos (`StereumWebhookController.java`):

```java
private void verificarFirma(String body, String signature) {
    String firmaEsperada = new HmacUtils(HmacAlgorithms.HMAC_SHA_256,
            apiKey.getBytes(StandardCharsets.UTF_8))
            .hmacHex(body.getBytes(StandardCharsets.UTF_8));   // recalcular
    if (!firmaEsperada.equalsIgnoreCase(signature)) {          // comparar
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Firma inválida");
    }
}
```

Detalles clave (muy preguntables):
- Se firma el **cuerpo crudo** (`String body`), no el objeto parseado. Por eso el controller recibe `@RequestBody String body` y **no** un DTO: si Jackson reserializara, cambiaría un espacio/orden y la firma no coincidiría. → **La firma se valida sobre los bytes exactos recibidos.**
- **Anti-replay con timestamp**: aunque la firma sea válida, un atacante podría re-enviar (replay) una petición vieja capturada. Por eso se exige el header `x-timestamp` y se rechaza si tiene más de 5 minutos:
  ```java
  if (Math.abs(ahora - timestamp) > TOLERANCIA_SEGUNDOS) // 5 min
      throw ... FORBIDDEN "Notificación expirada";
  ```
- **Mejora de seguridad** (buen punto para mencionar en el examen): `equalsIgnoreCase` no es *constant-time*; lo ideal sería `MessageDigest.isEqual()` para evitar *timing attacks*.

### 2.3 JWT — teoría

**JWT** = *JSON Web Token*. Es un string con **3 partes separadas por puntos**: `header.payload.signature`, cada una en Base64URL.

```
eyJhbGciOiJIUzI1NiJ9 . eyJzdWIiOiJhZG1pbkBjb20i...} . 4f2a...firma
   HEADER (algoritmo)      PAYLOAD (claims/datos)         SIGNATURE (HMAC)
```

- **Header**: el algoritmo, ej. `{"alg":"HS256"}`.
- **Payload**: los **claims** (datos). Estándar: `sub` (subject), `exp` (expiración), `iat` (emitido en), `jti` (id del token).
- **Signature**: `HMAC-SHA256(header + "." + payload, claveSecreta)`. ← **acá aparece HMAC otra vez.**

Propiedad central: el payload **no está cifrado**, solo firmado. Cualquiera puede leerlo (Base64), pero **nadie puede modificarlo** sin invalidar la firma (porque no tiene la clave secreta). Por eso **no se ponen secretos en el payload**.

### 2.4 JWT en el proyecto

**Creación** del token al hacer login (`JwtTokenProvider.createToken`):
```java
Claims claims = Jwts.claims()
    .subject(usuario.getUsername())    // sub = email
    .id(usuario.getId().toString())    // jti = id del usuario
    .issuedAt(now)
    .expiration(plusMinutes(now, 480)) // exp = 8 horas
    .build();
SecretKey key = Keys.hmacShaKeyFor(secretKeyBytes);  // clave HMAC desde Base64
String token = Jwts.builder().claims(claims).signWith(key).compact();
```
Se usa la librería **jjwt (0.12.6)**. La clave secreta está en `application.properties` (`security.jwt.token.secret-key`, en Base64, mínimo 256 bits para HS256).

**Validación** en cada request (`JwtTokenProvider.validateToken`, llamada desde `JwtTokenFilter`):
```java
Jws<Claims> claims = Jwts.parser()
    .verifyWith(key)               // verifica la firma HMAC con la clave
    .build()
    .parseSignedClaims(token);     // lanza excepción si la firma o el formato fallan
if (claims.getPayload().getExpiration().after(new Date())) { ... }  // chequea exp
Long userId = Long.parseLong(claims.getPayload().getId());          // jti
```
> 🔎 **Dato real útil:** el token de este proyecto **no guarda el rol** en los claims (solo `sub` y `jti`). Por eso, en el frontend, hay que decodificar el `jti` y pedir `GET /api/usuarios/{id}` para conocer el rol. Es un buen ejemplo de "qué poner y qué no poner en el JWT".

**El filtro que ata todo** (`JwtTokenFilter`, extiende `OncePerRequestFilter` = se ejecuta 1 vez por request):
1. Extrae el token del header `Authorization: Bearer xxx`.
2. Si no hay token → deja pasar (los endpoints públicos no lo necesitan).
3. Si el token está en la **blacklist** → 401.
4. Valida la firma + expiración → si OK, setea el usuario en el `SecurityContextHolder`.

### 2.5 Logout en un sistema stateless — la blacklist

Problema clásico de examen: *"Si el JWT es stateless y válido hasta que expira, ¿cómo hago logout?"* Un JWT no se puede "borrar" del lado del servidor. Solución de este proyecto: una **lista negra en memoria** (`TokenBlacklist`, un `ConcurrentHashMap<token, expiración>`). Al hacer logout se agrega el token; el filtro rechaza cualquier token de la lista. Las entradas se **purgan de forma perezosa** (al insertar/consultar) cuando ya expiraron — ojo, esto se relaciona con **Jobs** (§8): podría hacerse con un job programado.

---

## 3. JSON / JSONObject

JSON (*JavaScript Object Notation*) es el formato de intercambio de datos de la API. El temario dice "JSONObject", así que se cubren **las dos formas** de trabajar JSON en Java.

### 3.1 `org.json.JSONObject` (la API clásica, manual)

`JSONObject` es de la librería `org.json`. Construís/leés JSON **a mano**:
```java
// Construir
JSONObject obj = new JSONObject();
obj.put("nombre", "Auriculares");
obj.put("precio", 299.90);
JSONArray items = new JSONArray();
items.put(obj);

// Leer
JSONObject parsed = new JSONObject(textoJson);
String nombre = parsed.getString("nombre");
double precio = parsed.optDouble("precio", 0.0); // opt = con valor por defecto si no existe
```
Es útil cuando el JSON es **dinámico** (no tenés una clase fija) o querés tomar solo un par de campos. Desventaja: verboso y propenso a errores de tipeo en las claves.

### 3.2 Cómo lo hace REALMENTE este proyecto: Jackson + DTOs

> ⚠️ **Importante para el examen:** este proyecto **no usa `JSONObject`**. Usa **Jackson** (la librería de JSON por defecto de Spring Boot). Tenés que saber explicar la diferencia.

Con Jackson, el JSON se mapea **automáticamente** a objetos Java (DTOs). No escribís parsing manual:
- **Entrada** (request): `@RequestBody ProductoRequest req` → Spring usa Jackson para convertir el JSON del body en el objeto `ProductoRequest`.
- **Salida** (response): devolvés `ProductoResponse` y Spring lo serializa a JSON solo.
- **Renombrar campos** con `@JsonProperty` (en `LoginResponse`):
  ```java
  @JsonProperty("access_token")   // el JSON dice "access_token", el campo Java es accessToken
  private String accessToken;
  ```
  Por eso en Postman/el front leemos `json.access_token` y no `accessToken`.

**El caso especial del webhook** (donde se mezclan ambos mundos): el controller recibe el JSON como **String crudo** (para el HMAC, §2.2) y lo parsea **manualmente** con el `ObjectMapper` de Jackson:
```java
// StereumWebhookController
private final ObjectMapper objectMapper;        // motor de Jackson
StereumWebhookNotificacion n = objectMapper.readValue(body, StereumWebhookNotificacion.class);
```
`ObjectMapper.readValue(json, Clase.class)` es el equivalente Jackson de `new JSONObject(json)` pero tipado. (Dato fino: este proyecto usa Spring Boot 4 con **Jackson 3**, por eso el import es `tools.jackson.databind.ObjectMapper`.)

> 💡 **Resumen para el examen:** `JSONObject` = manejo manual/dinámico del JSON; **Jackson + DTOs** = mapeo automático y tipado (lo que Spring usa por debajo). Sabé cuándo conviene cada uno: DTOs para contratos fijos, JSONObject/`JsonNode` para JSON irregular o de terceros.

---

## 4. Stereum (integración con pasarela de pago)

**Stereum** es una pasarela de pagos que genera **QR de cobro**. Es un **sistema externo** que consumimos por HTTP. Conceptos:

- **Cliente HTTP saliente**: se usa **`RestClient`** (el cliente HTTP moderno de Spring, reemplazo de `RestTemplate`). En `StereumService.crearCargo`:
  ```java
  restClient.post()
     .uri(urlBase + "/api/v1/transactions/create-charge")
     .header("x-api-key", apiKey)              // autenticación: header fijo, NO JWT
     .body(request)
     .retrieve()
     .toEntity(StereumCreateChargeResponse.class);
  ```
- **Autenticación con API Key**: Stereum no usa login/token; solo un header `x-api-key`. (Contraste con el "sistema externo par" del proyecto, que sí hace login y cachea un JWT.)
- **Timeouts**: se configuran un *connect-timeout* (2s) y un *read-timeout* alto (20s, porque generar el QR tarda):
  ```java
  factory.setReadTimeout(Duration.ofMillis(readTimeout));
  ```
- **Idempotencia (`idempotency_key`)**: si no viene, se genera un `UUID`. Sirve para que, si reintentás la misma operación, Stereum **no cobre dos veces**. Concepto clave en pagos.

**Ciclo de pago completo** (cómo se conecta con todo lo demás):
1. `POST /api/pedidos/.../qr` → `PedidoService.generarQrPago` llama a Stereum, crea un `Pago` en estado `PENDIENTE` y guarda el `transaccionPasarelaId` (id de Stereum).
2. El cliente paga escaneando el QR.
3. Stereum notifica por **webhook** (§5).

---

## 5. Webhook

Un **webhook** es una **llamada HTTP "al revés"**: en vez de que *vos* preguntes "¿ya pagó?" cada X segundos (*polling*), el sistema externo **te llama a vos** cuando ocurre el evento. Es *push*, no *pull*. Mucho más eficiente y en tiempo real.

Características del webhook de este proyecto (`StereumWebhookController`, `POST /api/webhooks/stereum/outbound`):

- **Es público** (sin JWT): Stereum no tiene tu token. La seguridad se basa en **HMAC** (§2.2) + timestamp anti-replay.
- **Flujo de cada notificación**:
  1. Verificar firma HMAC → si no coincide, 403.
  2. Verificar timestamp (≤ 5 min) → anti-replay.
  3. Parsear el JSON (Jackson).
  4. Si es de tipo `"test"` (Stereum valida la URL) → responder 200 sin tocar la BD.
  5. Si es `"transaction"` → procesar el cambio de estado.
- **Mapeo de estados** (`StereumWebhookService`): busca el `Pago` por `transaccionPasarelaId` y:
  - Stereum `PAGADO` → `Pago = EXITOSO` y `Pedido = PAGADO`.
  - Stereum `CANCELADO/ERROR` → `Pago = RECHAZADO` (el pedido queda `PENDIENTE` para reintentar).
- **Idempotencia** (concepto crítico en webhooks): Stereum puede **reenviar** la misma notificación. Por eso, si el pago ya está cerrado, se ignora:
  ```java
  if ("EXITOSO".equals(pago.getEstadoPago()) || "RECHAZADO".equals(pago.getEstadoPago())) {
      log.info("El pago {} ya está en estado {} — notificación ignorada", ...);
      return;   // no reprocesar
  }
  ```
  > Procesar dos veces un pago duplicaría stock/estados → por eso **un webhook siempre debe ser idempotente**.

---

## 6. Transactions & métodos asíncronos

### 6.1 Transacciones (`@Transactional`) — teoría

Una **transacción** agrupa varias operaciones de BD en una unidad **atómica**: o se hacen **todas**, o **ninguna** (*all-or-nothing*). Cumple **ACID**: Atomicidad, Consistencia, Aislamiento, Durabilidad.

`@Transactional` en Spring funciona con un **proxy AOP**: Spring envuelve tu método; abre la transacción al entrar, hace **commit** si termina bien, y **rollback** si se lanza una excepción **runtime** (`RuntimeException`).

### 6.2 Transacciones en el proyecto — el ejemplo estrella

`PedidoService.crearDesdeCarrito` es el caso perfecto de atomicidad. Al crear un pedido pasan **varias** escrituras que **deben pasar juntas**:
```java
@Transactional
public PedidoResponse crearDesdeCarrito(CrearPedidoRequest request) {
    // por cada ítem:
    producto.setStock(producto.getStock() - dc.getCantidad());  // 1. descontar stock
    productoRepository.save(producto);
    pedido.getDetalles().add(dp);                               // 2. crear detalle
    movimientoRepository.save(mov);                            // 3. movimiento SALIDA
    // ...
    pedidoRepository.save(pedido);                             // 4. guardar pedido
    carrito.setEstado("CONVERTIDO_A_PEDIDO");                  // 5. cerrar carrito
}
```
Si en el ítem 3 de 5 productos **falla** (ej. "stock insuficiente"), la excepción provoca **rollback**: se revierte el stock ya descontado y no queda un pedido a medias. **Sin `@Transactional`, la BD quedaría inconsistente.**

Otros usos en el proyecto que tenés que reconocer:
- **`@Transactional(readOnly = true)`** en `ReporteService`/`DashboardService`: optimización para consultas (no hay escritura; le dice al driver que no prepare rollback). También mantiene la **sesión abierta** para leer relaciones *lazy* sin `LazyInitializationException`.
- **Webhook idempotente** (`@Transactional` en `procesarNotificacion`): actualizar `Pago` + `Pedido` juntos.
- **`cancelarPedido`**: repone stock + escribe movimientos `ENTRADA`, todo atómico.

**Trampas que te pueden preguntar (muy comunes):**
- **Self-invocation**: si un método llama a **otro método `@Transactional` de la misma clase**, el proxy **no se aplica** (la llamada es interna, no pasa por el proxy). La anotación se ignora.
- **Solo hace rollback con `RuntimeException`** por defecto (no con *checked exceptions*, salvo que uses `rollbackFor`).
- **OSIV (Open Session In View)**: Spring Boot lo tiene activado por defecto, lo que mantiene la sesión JPA abierta durante todo el request. Por eso en el proyecto el mapeo de relaciones *lazy* en los `fromEntity()` funciona aunque el método del servicio ya haya terminado.

### 6.3 Métodos asíncronos (`@Async`) — teoría

> ⚠️ **Honestidad:** este proyecto **no usa `@Async`** todavía. Teoría + dónde encajaría, porque está en el temario.

Por defecto, todo en Spring MVC es **síncrono**: el request espera a que el método termine. Un **método asíncrono** se ejecuta en **otro hilo (thread)**, liberando al request para que responda ya, sin esperar.

Se activa con `@EnableAsync` (en una `@Configuration`) y se marca el método con `@Async`:
```java
@EnableAsync
@Configuration class AsyncConfig {}

@Async
public CompletableFuture<Void> enviarNotificacion(...) { ... } // corre en otro hilo
```
- Puede devolver `void` o un **`CompletableFuture<T>`** (para conocer el resultado más tarde).
- **¿Cuándo se usa?** Para tareas lentas que no necesitan bloquear la respuesta: enviar emails, notificaciones, generar reportes pesados, llamadas a sistemas externos.
- **Dónde encajaría en ESTE proyecto:** procesar el webhook de Stereum en segundo plano y responder 200 a Stereum de inmediato; o enviar un correo de "pedido confirmado" tras un pago exitoso.

**Trampas (te las pueden preguntar):**
- Igual que `@Transactional`, `@Async` usa **proxy** → **no funciona en self-invocation** ni en métodos `private`.
- **Cuidado de combinar `@Async` con `@Transactional`**: el método async corre en **otro hilo**, con **otra transacción** y **otra sesión** JPA — no comparte la transacción del que lo llamó.

---

## 7. Logs

Un **log** es un registro de lo que hace la aplicación: sirve para **depurar, auditar y monitorear** en producción (donde no tenés un debugger).

**Stack del proyecto:** **SLF4J** (la *fachada*/API) + **Logback** (la implementación, por defecto en Spring Boot). Vos programás contra SLF4J; Logback hace el trabajo. Lombok provee `@Slf4j`, que te crea automáticamente un logger `log`:
```java
@Slf4j
@Service
public class StereumService {
    log.info("Cargo creado en Stereum OK");
    log.error("Exception al crear cargo en Stereum. ", e);   // loguea el stacktrace
}
```

**Niveles de log** (de menos a más grave) — clave de examen:
`TRACE < DEBUG < INFO < WARN < ERROR`. Configurás el umbral en `application.properties`:
```properties
logging.level.com.upb.ecommerce=DEBUG   # mostrar DEBUG y superiores de tu paquete
```
Si el nivel es `INFO`, los `debug()` no se imprimen.

**Buenas prácticas que tenés que saber:**
- **Logging parametrizado con `{}`** (NO concatenar con `+`):
  ```java
  log.info("Autenticando email={} tiendaId={}", email, tiendaId);   // ✅ eficiente
  log.info("Autenticando " + email + tiendaId);                     // ❌ siempre concatena
  ```
  Con `{}`, si el nivel está apagado, ni siquiera arma el String → más rápido.
- **Nunca loguear secretos**: contraseñas, tokens JWT, API keys. (En este proyecto se loguea el email pero no la contraseña — bien.)
- **Logueá la excepción como último argumento** (`log.error("msg", e)`) para que salga el *stacktrace* completo.
- Usá el **nivel adecuado**: `INFO` para eventos de negocio ("pedido pagado"), `WARN` para situaciones raras recuperables ("firma de webhook inválida"), `ERROR` para fallos reales.

---

## 8. Jobs

Un **job** es una tarea que **no la dispara un request HTTP**, sino el ciclo de vida de la app o un **reloj** (schedule).

### 8.1 Job de arranque: `CommandLineRunner` (sí se usa)

`DataSeeder` implementa `CommandLineRunner`: Spring ejecuta su método `run()` **una vez, al terminar de arrancar** la aplicación. Se usa para **sembrar datos** (acá crea el usuario admin `admin@comercio1.com`):
```java
@Component
public class DataSeeder implements CommandLineRunner {
    @Override public void run(String... args) {
        // crear admin si no existe
    }
}
```
(Alternativa equivalente: `ApplicationRunner`.)

### 8.2 Inicialización de bean: `@PostConstruct` (sí se usa)

No es un "job" programado, pero es ciclo de vida y te lo pueden mezclar. En `JwtTokenProvider`:
```java
@PostConstruct
protected void init() {
    secretKeyBytes = Base64.getDecoder().decode(secretKey);  // se ejecuta tras crear el bean
}
```
`@PostConstruct` corre **una vez**, después de que Spring inyectó las dependencias del bean.

### 8.3 Jobs programados: `@Scheduled` (teoría — no usado aún)

> ⚠️ **Honestidad:** este proyecto **no tiene `@Scheduled`**. Teoría + dónde encajaría:

Se activa con `@EnableScheduling` (en una `@Configuration`) y se marca el método con `@Scheduled`. Formas:
```java
@Scheduled(fixedRate = 60000)              // cada 60 s (desde que arranca cada ejecución)
@Scheduled(fixedDelay = 60000)             // 60 s después de que termina la anterior
@Scheduled(cron = "0 0 3 * * *")           // todos los días a las 03:00 (expresión cron)
public void tarea() { ... }
```
- El método debe ser `void` y sin parámetros.
- **Caso ideal en ESTE proyecto:** purgar la `TokenBlacklist` periódicamente. Hoy se limpia "de forma perezosa" (al insertar/consultar); un `@Scheduled(fixedRate = ...)` que llame a `purgeExpired()` sería la solución "de manual". Es el ejemplo perfecto para una pregunta de examen: *"¿cómo limpiarías tokens expirados de la blacklist con un job?"*

### 8.4 Job vs Async (no confundir)
- **Async (`@Async`)**: dispara una tarea en otro hilo **a partir de un request** (alguien lo llama).
- **Job (`@Scheduled`)**: se dispara **solo, por tiempo**, sin que nadie lo invoque.

---

## 9. Cómo se conecta todo (el flujo de una venta)

Para fijar los temas, seguí una compra de punta a punta y mirá qué tema toca cada paso:

1. **Login** → `POST /api/auth` valida con BCrypt y emite un **JWT** firmado con **HMAC-SHA256**. *(Seguridad, JWT/HMAC)*
2. Cada request lleva `Authorization: Bearer …`; el **`JwtTokenFilter`** lo valida y chequea la **blacklist**. *(Seguridad, JWT)*
3. El cliente arma carrito y crea el pedido → método **`@Transactional`** que descuenta stock + escribe movimientos **atómicamente**. *(Transactions)*
4. Se genera el QR llamando a **Stereum** con **RestClient** + `x-api-key` + `idempotency_key`. El **JSON** request/response lo mapea **Jackson** a DTOs. *(Stereum, JSON)*
5. Stereum confirma por **webhook** (público, validado con **HMAC** + timestamp), procesado de forma **idempotente** dentro de una **transacción**. *(Webhook, HMAC, Transactions)*
6. Todo el camino deja **logs** parametrizados por nivel. *(Logs)*
7. Al arrancar, un **`CommandLineRunner`** ya había sembrado el admin; un **`@Scheduled`** podría limpiar la blacklist. *(Jobs)*

---

## 10. Mini-cuestionario de repaso (autoevaluación)

1. ¿Por qué el webhook recibe `String` crudo y no un DTO? *(firma HMAC sobre bytes exactos)*
2. Si el JWT es stateless, ¿cómo funciona el logout? *(blacklist + filtro)*
3. ¿Qué pasa si un método `@Transactional` llama a otro `@Transactional` de la misma clase? *(self-invocation: no se aplica el proxy)*
4. Diferencia entre `JSONObject` y Jackson/DTOs. *(manual/dinámico vs automático/tipado)*
5. ¿Por qué un webhook debe ser idempotente? *(reenvíos: no procesar dos veces)*
6. ¿`@Async` vs `@Scheduled`? *(disparado por request en otro hilo vs disparado por tiempo)*
7. ¿Por qué loguear con `{}` y no con `+`? *(no arma el String si el nivel está apagado)*
8. ¿Qué es HMAC y qué garantiza? *(MAC con clave secreta: integridad + autenticidad)*
9. ¿Qué partes tiene un JWT y cuál se firma? *(header.payload.signature; la firma = HMAC del header+payload)*
10. ¿Por qué `@Transactional(readOnly = true)` en los reportes? *(optimización de solo lectura + sesión abierta para lazy)*

---

### Referencias rápidas a archivos del proyecto

| Tema | Archivo(s) |
|------|-----------|
| Seguridad | `ecommerce-api/.../config/SecurityConfig.java` |
| JWT (crear/validar) | `ecommerce-api/.../config/JwtTokenProvider.java`, `JwtTokenFilter.java` |
| Logout / blacklist | `ecommerce-api/.../config/TokenBlacklist.java`, `controller/AuthController.java` |
| HMAC / Webhook | `ecommerce-api/.../controller/StereumWebhookController.java` |
| Webhook (negocio) | `ecommerce-core/.../integracion/StereumWebhookService.java` |
| Stereum (RestClient) | `ecommerce-core/.../integracion/StereumService.java` |
| JSON (Jackson/DTO) | `ecommerce-core/.../dto/response/LoginResponse.java` |
| Transacciones | `ecommerce-core/.../service/PedidoService.java` |
| Logs | cualquier clase con `@Slf4j` (ej. `StereumService`, `AuthController`) |
| Jobs (arranque) | `ecommerce-data/.../seeders/DataSeeder.java` |
| Init de bean | `ecommerce-api/.../config/JwtTokenProvider.java` (`@PostConstruct`) |
