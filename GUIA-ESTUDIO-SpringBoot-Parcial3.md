# Guía de Estudio — Spring Boot (Tercer Parcial)

> Material basado en los **tres bloques de cambios** que se integraron a este proyecto **ecommerce** (multi-tienda con pagos por QR). Explica cada tema con **teoría**, **el porqué** y **el código real** de este proyecto donde aparece.
>
> El proyecto es **multi-módulo Maven**: `ecommerce-domain` (entidades) → `ecommerce-data` (repositorios) → `ecommerce-core` (servicios + DTOs + excepciones) → `ecommerce-api` (controladores + seguridad + config). Cada concepto suele tocar varias capas.

## Índice
- [Bloque 1 — Webhooks, HMAC e integración con un sistema externo (Stereum)](#bloque-1)
  1. [Webhook (HTTP entrante de sistema a sistema)](#1-webhook)
  2. [HMAC — verificación de firma](#2-hmac)
  3. [Anti-replay con timestamp](#3-timestamp)
  4. [Consumir una API externa con `RestClient` + DTOs](#4-restclient)
  5. [Idempotencia en el procesamiento](#5-idempotencia)
- [Bloque 2 — Métodos asíncronos con valor de retorno (Future) y Jobs con trabajadores](#bloque-2)
  6. [`Future` / `CompletableFuture` — async que devuelve resultado](#6-future)
  7. [Patrón scatter-gather (trabajadores en paralelo)](#7-scatter-gather)
  8. [Paginación: `Page`, `Pageable`, `PageRequest`, `Sort`](#8-paginacion)
  9. [Job programado (`@Scheduled`) que coordina trabajadores](#9-job)
  10. [Auditoría JPA (`@LastModifiedDate`) para detectar abandono](#10-auditoria)
- [Bloque 3 — Excepciones personalizadas y manejo de errores HTTP](#bloque-3)
  11. [Excepciones personalizadas (`OperationException`, `NotDataFoundException`)](#11-excepciones)
  12. [Manejo centralizado con `@RestControllerAdvice`](#12-advice)
  13. [Problem Details (RFC 7807)](#13-problem-details)
- [Mapa de conceptos para el examen](#mapa)

---

# BLOQUE 1
## Webhooks, HMAC e integración con un sistema externo (Stereum)

Este bloque agregó la **integración con Stereum** (pasarela de pagos por QR): generar un cobro en Stereum y recibir de vuelta, de forma segura, la notificación de que el pago se completó.

**El flujo de pago en este proyecto:**
1. `POST /api/pedidos/tienda/{tiendaId}/{pedidoId}/qr` → llamamos a Stereum, que devuelve un QR, y guardamos un `Pago` en estado `PENDIENTE` con el id de transacción de Stereum.
2. El cliente paga el QR.
3. Stereum nos avisa con un **webhook** (`POST /api/webhooks/stereum/outbound`).
4. Verificamos la firma → actualizamos el `Pago` a `EXITOSO` y el `Pedido` a `PAGADO`.

---

### 1. Webhook

#### Teoría
Un **webhook** es un endpoint HTTP que **expones para que OTRO sistema te llame automáticamente** cuando ocurre un evento (aquí: "el cargo cambió de estado"). Es comunicación **máquina-a-máquina**, sin un humano ni un navegador.

- **API normal**: TÚ llamas a un servidor cuando necesitas algo (modelo *pull*).
- **Webhook**: el otro servidor te llama a TI cuando algo pasa (modelo *push*).

#### Práctica — `StereumWebhookController`
```java
@RestController
@RequestMapping("/api/webhooks/stereum")
public class StereumWebhookController {

    @Value("${stereum.api-key}")
    private String apiKey;   // misma llave que usamos para llamar a Stereum

    @PostMapping(value = "/outbound", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> outbound(
            @RequestHeader("x-signature") String signature,   // firma que envía Stereum
            @RequestHeader("x-timestamp") long timestamp,
            @RequestBody String body) {                        // cuerpo CRUDO (texto)
        verificarFirma(body, signature);   // 1. ¿es auténtico?
        verificarTiempo(timestamp);        // 2. ¿es reciente?
        StereumWebhookNotificacion n = parsear(body);  // 3. texto → objeto
        // 4-5. según el tipo, procesar...
    }
}
```
- **`@RequestHeader`**: lee cabeceras HTTP que el sistema externo manda (aquí `x-signature` y `x-timestamp`).
- **`@RequestBody String body`**: recibe el cuerpo **como texto crudo** (no como objeto). Esto es **clave** para webhooks firmados: la firma se calcula sobre los bytes *exactos* del body, así que hay que leerlo tal cual llegó y firmarlo **antes** de convertirlo a objeto. (Si dejaras que Jackson lo deserializara primero, al volver a serializar podrían cambiar espacios/orden de campos y la firma no cuadraría.)
- Este endpoint es **público** (no requiere JWT). Está permitido en `SecurityConfig`:
  ```
  POST /api/webhooks/stereum/outbound → permitAll()
  ```
  Pero "público" no significa "inseguro": la seguridad la da la **firma HMAC** (siguiente tema), no el JWT de usuario.

#### Para el examen
- Webhook = endpoint que TÚ expones para recibir llamadas automáticas de otro sistema (modelo *push*).
- Se lee el body como `String` crudo cuando hay verificación de firma.
- Suele ser público pero protegido por firma, no por token de usuario.

---

### 2. HMAC

#### Teoría
**HMAC (Hash-based Message Authentication Code)** responde a: *"¿este mensaje vino realmente de quien dice, y nadie lo alteró en el camino?"*

Funciona con una **clave secreta compartida** que solo conocen los dos sistemas (nuestro ecommerce y Stereum):
1. Stereum calcula `firma = HMAC_SHA256(cuerpo, clave_secreta)` y la manda en `x-signature`.
2. Nuestro sistema **recalcula** la misma firma con su copia de la clave.
3. Si ambas coinciden → mensaje auténtico e íntegro. Si no → se rechaza con **403**.

> Es el **mismo concepto del JWT** (parcial anterior): firma simétrica con clave secreta. La diferencia es **dónde** se aplica: el JWT firma el token de login; aquí se firma el cuerpo del webhook.

#### Práctica — `verificarFirma` en `StereumWebhookController`
```java
private void verificarFirma(String body, String signature) {
    String firmaEsperada = new HmacUtils(HmacAlgorithms.HMAC_SHA_256,
            apiKey.getBytes(StandardCharsets.UTF_8))    // la clave secreta
            .hmacHex(body.getBytes(StandardCharsets.UTF_8));  // se firma el body crudo

    if (!firmaEsperada.equalsIgnoreCase(signature)) {   // ¿calculada == recibida?
        log.warn("Firma de webhook inválida — se rechaza la petición");
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Firma inválida");
    }
}
```
- **`HmacUtils`** (Apache Commons Codec): utilidad que calcula el HMAC.
- **`hmacHex(...)`**: devuelve la firma como texto hexadecimal (para compararla con la cabecera).
- **`equalsIgnoreCase`**: la firma hex puede venir en mayúsculas o minúsculas.
- Si la firma no cuadra, se lanza **403 Forbidden** y el pago **no** se procesa.

La clave secreta vive en `application.properties` (es el mismo `stereum.api-key` que usamos para llamar a Stereum):
```properties
stereum.api-key=98ca7eb3-7ce4-4bcb-8785-21dd15de4477
```

#### Para el examen
- HMAC = firma con **clave secreta simétrica** para garantizar **autenticidad + integridad**.
- `HMAC_SHA_256` = algoritmo HMAC usando SHA-256 como función hash.
- El receptor **recalcula** la firma y la compara; nunca "descifra" nada (HMAC no es cifrado).
- Se firma sobre el **body crudo** exacto.

---

### 3. Anti-replay con timestamp

#### Teoría
Aunque la firma sea válida, un atacante podría **capturar** una petición legítima y **reenviarla** después (*replay attack*). Para evitarlo, cada notificación trae `x-timestamp` (hora en que se generó) y rechazamos las demasiado viejas.

#### Práctica — `verificarTiempo`
```java
private static final long TOLERANCIA_SEGUNDOS = 5 * 60; // 5 minutos

private void verificarTiempo(long timestamp) {
    long ahora = Instant.now().getEpochSecond();
    if (Math.abs(ahora - timestamp) > TOLERANCIA_SEGUNDOS) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Notificación expirada");
    }
}
```
- `Instant.now().getEpochSecond()` = segundos desde 1970 (epoch).
- Si la diferencia con el timestamp recibido supera 5 minutos → **403**.

#### Para el examen
- El timestamp evita *replays*: una petición vieja (aunque bien firmada) se rechaza.
- Se compara con una tolerancia (aquí 5 min) por desfases de reloj entre servidores.

---

### 4. Consumir una API externa con `RestClient` + DTOs

#### Teoría
Para **llamar** a Stereum (no recibir, sino pedir un cobro) se usa **`RestClient`**, el cliente HTTP moderno de Spring. A diferencia de un webhook (donde leemos texto crudo), aquí mandamos y recibimos **DTOs** que Jackson convierte a/desde JSON automáticamente.

> En este proyecto conviven las dos formas de manejar JSON: en el webhook se lee el body como `String` crudo (para poder firmarlo), mientras que para llamar a Stereum se usan DTOs tipados (`StereumCreateChargeRequest`/`Response`).

#### Práctica — `StereumService.crearCargo`
```java
public StereumCreateChargeResponse crearCargo(StereumCreateChargeRequest request) throws Exception {
    if (request.getIdempotencyKey() == null || request.getIdempotencyKey().isBlank()) {
        request.setIdempotencyKey(UUID.randomUUID().toString());   // clave de idempotencia
    }
    RestClient restClient = create();   // RestClient con timeouts

    ResponseEntity<StereumCreateChargeResponse> response = restClient.post()
            .uri(urlBase + "/api/v1/transactions/create-charge")
            .header("x-api-key", apiKey)     // Stereum NO usa login/token, sino una API key fija
            .body(request)                   // DTO → JSON (Jackson)
            .retrieve()
            .toEntity(StereumCreateChargeResponse.class);   // JSON → DTO

    if (!response.getStatusCode().is2xxSuccessful()) {
        throw new Exception("Se genero error");
    }
    return response.getBody();
}
```
Los **timeouts** se configuran al crear el cliente (la generación del QR puede tardar, por eso el read-timeout es alto):
```java
private RestClient create() {
    SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
    f.setConnectTimeout(Duration.ofMillis(connectTimeout)); // 5s
    f.setReadTimeout(Duration.ofMillis(readTimeout));       // 20s
    return RestClient.builder().requestFactory(f).build();
}
```

#### Para el examen
- `RestClient` = cliente HTTP de Spring: `.post().uri(...).body(dto).retrieve().toEntity(Clase.class)`.
- Mandar/recibir DTOs = Jackson convierte JSON↔objeto automáticamente (tipado, lo opuesto al body crudo del webhook).
- Stereum se autentica con header fijo `x-api-key` (no con login/JWT).
- Timeouts: `connectTimeout` (tardar en conectar) vs `readTimeout` (tardar en responder).

---

### 5. Idempotencia en el procesamiento

#### Teoría
**Idempotente** = ejecutar la misma operación varias veces produce el mismo resultado que ejecutarla una. Stereum **puede reenviar** la misma notificación (reintentos de red), así que el procesamiento debe ignorar duplicados.

#### Práctica — `StereumWebhookService.procesarNotificacion`
```java
Pago pago = pagoRepository.findByTransaccionPasarelaId(tx.getId()).orElse(null);
if (pago == null) { return; }   // transacción que no conocemos → ignorar

// Idempotencia: si ya está cerrado, no reprocesar.
if ("EXITOSO".equals(pago.getEstadoPago()) || "RECHAZADO".equals(pago.getEstadoPago())) {
    return;
}

switch (tx.getStatus()) {
    case "PAGADO" -> {                     // Stereum confirmó el pago
        pago.setEstadoPago("EXITOSO");
        pago.getPedido().setEstadoPedido("PAGADO");
    }
    case "CANCELADO", "ERROR" -> pago.setEstadoPago("RECHAZADO"); // pedido sigue PENDIENTE (reintentable)
    default -> { /* PENDIENTE: sin cambios */ }
}
```
- `transaccionPasarelaId` (el id de Stereum) es la **clave** para encontrar nuestro `Pago`.
- Si el pago ya está `EXITOSO`/`RECHAZADO`, la notificación repetida **se ignora**.

#### Para el examen
- Idempotencia = procesar duplicados sin efectos secundarios (clave en webhooks, que se reenvían).
- Se logra revisando el estado actual antes de actuar.

---

# BLOQUE 2
## Métodos asíncronos con valor de retorno (Future) y Jobs con trabajadores

Este bloque introdujo el **barrido de carritos abandonados**: cada 8 horas, buscar carritos que el cliente dejó a medias, mandarle un correo recordatorio (en paralelo) y marcarlos como `ABANDONADO`. Junta cuatro conceptos: **async con resultado**, **paralelismo**, **paginación** y **jobs programados**.

---

### 6. `Future` / `CompletableFuture`

#### Teoría
Un método `@Async` corre en **otro hilo**. Si es `void` es "dispara y olvida". Pero a veces **sí necesitas el resultado** o saber cuándo terminó. Para eso el método async devuelve un **`Future<T>`**.

- **`Future<T>`** = una "promesa" de un valor `T` que estará disponible **en el futuro**. El método devuelve el `Future` de inmediato (sin bloquear).
- **`future.get()`** = "dame el resultado; si todavía no está listo, **espérame aquí**" (operación **bloqueante**).
- **`CompletableFuture.completedFuture(valor)`** = empaqueta un valor ya calculado dentro de un `Future`.

Para que `@Async` funcione hay que **habilitarlo** y darle un **pool de hilos** (`InjectConfiguration`):
```java
@Configuration
@EnableAsync          // habilita @Async
@EnableScheduling     // habilita @Scheduled
@EnableJpaAuditing    // habilita @LastModifiedDate
public class InjectConfiguration {

    @Bean
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);   // 5
        executor.setMaxPoolSize(maxPoolSize);     // 5
        executor.setQueueCapacity(queueCapacity); // 50
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
    }
}
```

#### Práctica — `CarritoService.procesarCarritoAbandonado`
```java
@Async
@Transactional
public Future<Long> procesarCarritoAbandonado(Long carritoId) {
    Carrito carrito = carritoRepository.findById(carritoId).orElse(null);
    // Re-validamos dentro del hilo: pudo cambiar de estado mientras esperaba en la cola.
    if (carrito == null || !"ACTIVO".equals(carrito.getEstado())) {
        return CompletableFuture.completedFuture(carritoId);
    }

    int cantidadItems = carrito.getDetalles() == null ? 0 : carrito.getDetalles().size();
    if (cantidadItems > 0) {
        try {
            emailService.enviarRecordatorioCarrito(...);  // enviar correo
        } catch (Exception e) {
            // Un fallo de correo NO debe impedir marcar el carrito como abandonado.
            log.warn("No se pudo enviar el correo del carrito {}: {}", carritoId, e.getMessage());
        }
    }

    carrito.setEstado("ABANDONADO");
    carritoRepository.save(carrito);
    return CompletableFuture.completedFuture(carritoId);  // devuelve el id procesado
}
```
- Corre en un hilo `async-X` (por `@Async`) y devuelve un `Future<Long>` al instante.
- **Detalle fino**: el `try/catch` alrededor del correo **aísla** el fallo de email — si el SMTP falla, igual se marca el carrito como `ABANDONADO`.

#### Para el examen
- `void` async = dispara y olvida. `Future<T>` async = dispara y **puedes recoger el resultado**.
- `future.get()` **bloquea** hasta que el resultado esté listo.
- `CompletableFuture.completedFuture(x)` envuelve un valor en un `Future`.
- `@Async` necesita `@EnableAsync` + un `ThreadPoolTaskExecutor`.

---

### 7. Patrón scatter-gather (trabajadores en paralelo)

#### Teoría
**Scatter-gather** ("dispersar y recolectar"):
1. **Scatter**: lanzar **muchas** tareas async a la vez (cada una un trabajador en su hilo).
2. **Gather**: esperar a que **todas** terminen, recogiendo sus resultados.

La ganancia es el **paralelismo**:
```
Secuencial:  correo(1) → correo(2) → correo(3)  = suma de los tres
Paralelo:    correo(1) ┐
             correo(2) ┤ todos a la vez = el tiempo del más lento
             correo(3) ┘
```

#### Práctica — dentro de `CarritoAbandonadoJob` (código completo en el tema 9)
```java
// SCATTER: lanzar un trabajador @Async por carrito (no esperamos aquí)
List<Future<Long>> trabajadores = new ArrayList<>();
for (Carrito carrito : pagina.getContent()) {
    trabajadores.add(carritoService.procesarCarritoAbandonado(carrito.getId()));
}

// GATHER: ahora sí, esperar a que todos terminen
for (Future<Long> trabajador : trabajadores) {
    try {
        Long id = trabajador.get();   // bloquea hasta que ESE trabajador termina
        totalProcesados++;
    } catch (Exception e) {
        log.warn("Error procesando un carrito del lote {}: {}", lote, e.getMessage());
    }
}
```
**El truco está en separar los dos bucles**:
- En el **primer for** se *arrancan* todos los trabajadores (no se espera a ninguno) → corren en paralelo.
- En el **segundo for** se *recogen* los resultados → aquí se espera, pero como ya estaban corriendo juntos, el tiempo total es el del más lento, no la suma.

> Si llamaras `procesarCarritoAbandonado(...).get()` dentro de **un solo** bucle, **perderías el paralelismo** (esperarías cada uno antes de lanzar el siguiente = secuencial otra vez).

#### Para el examen
- Scatter = lanzar todas las tareas; Gather = esperarlas todas con `.get()`.
- **Dos bucles separados** = paralelo. Un solo bucle con `.get()` dentro = secuencial.
- El `ThreadPoolTaskExecutor` provee los "trabajadores".

---

### 8. Paginación: `Page`, `Pageable`, `PageRequest`, `Sort`

#### Teoría
**Paginar** = traer los datos **de a páginas** (ej: 10 por vez) en vez de todos de golpe. Imprescindible con tablas grandes (no cargas miles de productos/carritos en memoria).

| Clase | Qué es |
|---|---|
| **`Pageable`** | La *petición* de página: "quiero la página 2, tamaño 10, ordenada por nombre". |
| **`PageRequest.of(pagina, tamaño, sort)`** | La forma de **construir** un `Pageable`. |
| **`Sort`** | El criterio de orden (`Sort.by(dir, "nombre")`). |
| **`Page<T>`** | El *resultado*: los registros de esa página + metadatos (total páginas, total elementos). |

#### Práctica A — endpoint paginado de productos
Repositorio (`ProductoRepository`) — basta con declarar el método sobrecargado recibiendo `Pageable`:
```java
Page<Producto> findByTiendaIdAndEstadoTrue(Long tiendaId, Pageable pageable);
```
Servicio (`ProductoService`) — `.map(...)` convierte `Page<Producto>` → `Page<ProductoResponse>` manteniendo los metadatos:
```java
@Transactional(readOnly = true)
public Page<ProductoResponse> listarPorTiendaPaginado(Long tiendaId, Pageable pageable) {
    return productoRepository.findByTiendaIdAndEstadoTrue(tiendaId, pageable)
            .map(ProductoResponse::fromEntity);
}
```
Controlador (`ProductoController`) — los parámetros de paginación llegan por la URL con `@RequestParam`:
```java
@GetMapping("/tienda/{tiendaId}/paginado")
public ResponseEntity<Page<ProductoResponse>> listarPorTiendaPaginado(
        @PathVariable Long tiendaId,
        @RequestParam(value = "page", defaultValue = "0") Integer page,
        @RequestParam(value = "size", defaultValue = "10") Integer size,
        @RequestParam(value = "sortBy", defaultValue = "nombre") String sortBy,
        @RequestParam(value = "sortDir", defaultValue = "ASC") Sort.Direction sortDir) {
    return ResponseEntity.ok(productoService.listarPorTiendaPaginado(
            tiendaId, PageRequest.of(page, size, Sort.by(sortDir, sortBy))));
}
```
Se llama así: `GET /api/productos/tienda/1/paginado?page=0&size=5&sortBy=precio&sortDir=DESC`
- **`@RequestParam`**: lee parámetros de la URL (query params).
- **`defaultValue`**: si no mandas el parámetro, usa ese valor.
- **`Sort.Direction`**: Spring convierte el texto `"DESC"` directamente al enum.

#### Práctica B — paginación dentro del barrido (`CarritoRepository`)
```java
Page<Carrito> findByEstadoAndFechaActualizacionBefore(String estado, LocalDateTime limite, Pageable pageable);
```

#### Para el examen
- `Pageable` = lo que pides; `Page<T>` = lo que recibes (con metadatos).
- `PageRequest.of(nº, tamaño, Sort)` construye la petición.
- `page.getContent()` (los datos) / `page.getTotalPages()` / `page.isEmpty()`.
- `Page.map(...)` transforma el contenido conservando la paginación (entidad → DTO).

---

### 9. Job programado (`@Scheduled`) que coordina trabajadores

Aquí se **juntan todos los conceptos**: un `@Scheduled` (job) que pagina (`Page`) y procesa con trabajadores async (`Future`).

```java
@Scheduled(cron = "${carrito.abandono.cron:0 0 */8 * * *}")  // 00:00, 08:00, 16:00
public void barrerCarritosAbandonados() {
    LocalDateTime limite = LocalDateTime.now().minusHours(horasAbandono);  // hace 1h

    // Siempre página 0: al marcar un carrito ABANDONADO deja de cumplir el filtro
    // (sale del conjunto ACTIVO), así que el siguiente lote vuelve a quedar al inicio.
    Pageable pageable = PageRequest.of(0, TAMANIO_LOTE, Sort.by("fechaActualizacion").ascending());

    int lote = 0, totalProcesados = 0;
    Page<Carrito> pagina;
    do {
        // 1) PAGINAR: pedir el lote de carritos abandonados
        pagina = carritoRepository.findByEstadoAndFechaActualizacionBefore("ACTIVO", limite, pageable);
        if (pagina.isEmpty()) break;

        // 2) SCATTER: lanzar un trabajador async por carrito
        List<Future<Long>> trabajadores = new ArrayList<>();
        for (Carrito carrito : pagina.getContent()) {
            trabajadores.add(carritoService.procesarCarritoAbandonado(carrito.getId()));
        }

        // 3) GATHER: esperar a que todos terminen
        for (Future<Long> trabajador : trabajadores) {
            try { trabajador.get(); totalProcesados++; }
            catch (Exception e) { log.warn("Error en el lote {}: {}", lote, e.getMessage()); }
        }
        lote++;
    } while (lote < MAX_LOTES);   // 4) siguiente lote (tope de seguridad)
}
```

#### Las piezas trabajando juntas
1. **Job (`@Scheduled`)**: el reloj dispara el método cada 8 horas.
2. **Paginación (`Page`)**: procesa los carritos por lotes de 50, sin cargarlos todos.
3. **Scatter (`@Async` + `Future`)**: envía los correos en paralelo (hilos `async-X`).
4. **Gather (`future.get()`)**: espera a que el lote termine antes de pedir el siguiente.

#### Diferencia clave con el ejemplo de clase (¡puede caer!)
En el ejemplo del docente el índice de página **aumenta** (`index++`) porque los registros siguen ahí. **Aquí siempre se lee la página 0** porque cada carrito procesado **sale del filtro** (pasa de `ACTIVO` a `ABANDONADO`), así que el conjunto que cumple la condición se va vaciando desde el inicio. Por eso hay un `MAX_LOTES` como tope anti-bucle-infinito.

#### El cron y la ventana de abandono (`application.properties`)
```properties
carrito.abandono.horas=1                 # ACTIVO sin tocar por >1h = abandonado
carrito.abandono.cron=0 0 */8 * * *      # seg min hora dia mes diaSemana
```
Formato cron de Spring: **6 campos** `segundo minuto hora día mes díaDeSemana`. `0 0 */8 * * *` = segundo 0, minuto 0, cada 8 horas.

#### Para el examen
- Un job puede combinar: scheduling + paginación + async con Future.
- Orden: paginar → lanzar trabajadores → esperar resultados → siguiente lote.
- Cron de Spring tiene **6 campos** (incluye segundos), no 5 como el cron de Unix.

---

### 10. Auditoría JPA (`@LastModifiedDate`) para detectar abandono

#### Teoría
Para saber si un carrito está "abandonado" necesitamos saber **cuándo se modificó por última vez**. JPA puede rellenar ese campo **solo** en cada `save` con la **auditoría**.

#### Práctica — entidad `Carrito`
```java
@Entity
@Table(name = "carritos")
@EntityListeners(AuditingEntityListener.class)   // activa la auditoría en esta entidad
public class Carrito {

    @LastModifiedDate
    @Column(name = "fecha_actualizacion")        // nullable a propósito (ver abajo)
    private LocalDateTime fechaActualizacion;
    ...
}
```
- **`@EntityListeners(AuditingEntityListener.class)`** + **`@EnableJpaAuditing`** (en `InjectConfiguration`) = JPA rellena `@LastModifiedDate` en cada guardado.
- **`@LastModifiedDate`**: se actualiza sola con la fecha/hora de la última modificación.
- **Por qué nullable**: las filas que ya existían (cargadas desde el `.backup`) no tienen valor. Como `NULL` nunca cumple `fechaActualizacion < limite`, esos carritos viejos **nunca** se barren por error.

#### Para el examen
- `@LastModifiedDate` + `@EntityListeners(AuditingEntityListener.class)` + `@EnableJpaAuditing` = fecha de modificación automática.
- Columnas nuevas en una BD con datos existentes → **nullable** para no romper las filas viejas (regla de este proyecto, con `ddl-auto=update`).

---

# BLOQUE 3
## Excepciones personalizadas y manejo de errores HTTP

Este bloque mejoró **cómo la API comunica los errores**. Antes muchos fallos devolvían un `500` genérico o un `400` que culpaba al cliente aunque la falla fuera del servidor. Ahora se distingue: recurso inexistente → **404**, datos/regla de negocio inválida (culpa del cliente) → **400**, fallo inesperado (culpa del servidor) → **500**, con un mensaje claro y un formato JSON uniforme.

---

### 11. Excepciones personalizadas

#### Teoría
Una **excepción personalizada** es una clase de error **propia** que representa una situación concreta. Sirve para **distinguir** tus errores controlados de los genéricos de Java/Spring, y para mapear cada uno a un código HTTP distinto.

Este proyecto tiene **dos**, ambas `extends RuntimeException`:

```java
// 404 — el recurso no existe
public class NotDataFoundException extends RuntimeException {
    public NotDataFoundException(String message) { super(message); }
}

// 400 — datos o reglas de negocio inválidas (culpa del cliente)
public class OperationException extends RuntimeException {
    public OperationException(String message) { super(message); }
}
```

**¿Por qué `RuntimeException` y no `Exception`?**

| | `extends Exception` (checked) | `extends RuntimeException` (unchecked) |
|---|---|---|
| ¿Obliga a `throws` o `try/catch`? | Sí, el compilador te obliga | No, es opcional |
| ¿Hace rollback en `@Transactional`? | **No** (necesita `rollbackFor`) | **Sí**, automáticamente |
| Uso típico | Errores recuperables previstos | Errores de lógica/validación |

Al ser `RuntimeException`, no ensucian las firmas con `throws` y **disparan el rollback de la transacción automáticamente** (tema de transacciones del parcial 2).

#### Práctica — lanzarlas en los servicios
`NotDataFoundException` (404) cuando algo no existe — en casi todos los servicios:
```java
Tienda tienda = tiendaRepository.findById(request.getTiendaId())
        .orElseThrow(() -> new NotDataFoundException("Tienda no encontrada"));
```
`OperationException` (400) cuando se viola una regla de negocio — por ejemplo en `PedidoService`:
```java
if (producto.getStock() < dc.getCantidad()) {
    throw new OperationException("Stock insuficiente para: " + producto.getNombre());
}
if ("PAGADO".equals(pedido.getEstadoPedido())) {
    throw new OperationException("El pedido ya fue pagado");
}
```
o en `CarritoService`:
```java
if (producto.getStock() < request.getCantidad()) {
    throw new OperationException("Stock insuficiente. Disponible: " + producto.getStock());
}
```
El servicio **lanza** la excepción con un mensaje claro; **no decide** el código HTTP (eso lo hace el manejador global). Separación de responsabilidades: el servicio sabe de *negocio*, el manejador sabe de *HTTP*.

#### Para el examen
- Excepción personalizada = clase propia que `extends RuntimeException`.
- `RuntimeException` (unchecked): no obliga a `throws` y **sí** hace rollback automático.
- Este proyecto: `NotDataFoundException` → 404, `OperationException` → 400.
- El servicio **lanza**; el manejador global **decide el HTTP**.

---

### 12. Manejo centralizado con `@RestControllerAdvice`

#### Teoría
En vez de poner `try/catch` en **cada** controlador, un **`@RestControllerAdvice`** centraliza el manejo de errores de **toda** la aplicación. Cada `@ExceptionHandler` atrapa un tipo de excepción y devuelve la respuesta HTTP adecuada.

> Es una alternativa más limpia al `try/catch` + `ResponseStatusException` por controlador: una sola clase maneja los errores de todos.

#### Práctica — `GlobalExceptionHandler`
```java
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotDataFoundException.class)        // 404
    public ProblemDetail handleNotFound(NotDataFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Recurso no encontrado", ex.getMessage());
    }

    @ExceptionHandler(OperationException.class)           // 400 (culpa del cliente)
    public ProblemDetail handleOperation(OperationException ex) {
        log.warn("Operación inválida: {}", ex.getMessage());
        return problem(HttpStatus.BAD_REQUEST, "Solicitud inválida", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)  // 400 de @Valid
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> errores.put(e.getField(), e.getDefaultMessage()));
        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "Validación fallida",
                "Hay campos inválidos en la solicitud.");
        pd.setProperty("errors", errores);   // detalle por campo
        return pd;
    }

    @ExceptionHandler(InvalidJwtAuthenticationException.class) // 401
    public ProblemDetail handleInvalidJwt(InvalidJwtAuthenticationException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "No autorizado", ex.getMessage());
    }

    // Respeta el código que ya fijó el controller (p. ej. el webhook lanza 403).
    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        return problem(status, status.getReasonPhrase(), ex.getReason());
    }

    // Red de seguridad: cualquier error no contemplado = culpa del servidor (500).
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        log.error("Error inesperado del servidor", ex);   // se registra, pero NO se expone al cliente
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno",
                "Ocurrió un error interno. Inténtalo de nuevo más tarde.");
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail != null ? detail : title);
        pd.setTitle(title);
        return pd;
    }
}
```

**Decisiones de diseño (caen en examen):**
- El catch-all es **`Exception.class` → 500**, NO `RuntimeException.class` → 400. Un `NullPointerException` o un fallo de BD es culpa del servidor, no del cliente. Mapearlo a 400 sería mentirle al cliente.
- **`@ExceptionHandler(ResponseStatusException.class)`** deja pasar el código que ya decidió un controlador (el webhook lanza 403; el `AuthController` lanza 401/502). Sin este handler, esos códigos se convertirían en 500.
- En 500 se **registra** la causa real en el log (`log.error(..., ex)`) pero **no** se expone al cliente (no filtrar detalles internos).

#### Tabla de códigos HTTP en este proyecto
| Código | Cuándo | Origen |
|---|---|---|
| **200/201** | OK / creado | Operación exitosa |
| **400** | Datos o regla de negocio inválida | `OperationException`, `@Valid` |
| **401** | Token ausente/ inválido, credenciales malas | `InvalidJwtAuthenticationException`, `AuthController` |
| **403** | Firma de webhook inválida; rol incorrecto | webhook, `@PreAuthorize` |
| **404** | El recurso no existe | `NotDataFoundException` |
| **500** | Fallo inesperado | catch-all `Exception` |
| **502** | El sistema externo falló | `AuthController` (`ResponseStatusException`) |

#### Para el examen
- `@RestControllerAdvice` + `@ExceptionHandler` = manejo de errores centralizado para toda la API.
- El catch-all debe ser `Exception` → 500 (no `RuntimeException` → 400).
- 4xx = culpa del cliente; 5xx = culpa del servidor.
- Pasar `ResponseStatusException` tal cual respeta el código fijado por el controlador.

---

### 13. Problem Details (RFC 7807)

#### Teoría
**Problem Details** es un **estándar** (RFC 7807) para que TODOS los errores de una API tengan el **mismo formato JSON**, en vez de que cada error tenga su propia estructura. Así el frontend siempre sabe dónde leer el mensaje.

En Spring se representa con la clase **`ProblemDetail`** (lo que devuelve cada handler de arriba). Además, una línea en `application.properties` hace que **también** los errores del propio framework (404 de ruta inexistente, 405, etc.) salgan en ese formato:
```properties
spring.mvc.problem-details.enabled=true
```

#### Práctica — la respuesta que recibe el cliente
Si pides un producto que no existe, en vez de un error sin formato recibes:
```json
{
  "type": "about:blank",
  "title": "Recurso no encontrado",
  "status": 404,
  "detail": "Producto no encontrado",
  "instance": "/api/productos/tienda/1/999"
}
```
Y un error de validación (`@Valid`) agrega la propiedad extra `errors`:
```json
{
  "title": "Validación fallida",
  "status": 400,
  "detail": "Hay campos inválidos en la solicitud.",
  "errors": { "precio": "no debe ser nulo", "nombre": "no debe estar vacío" }
}
```
- **`status`** → el código. **`title`** → nombre corto. **`detail`** → tu mensaje (el de la excepción): **aquí lee el frontend**. **`instance`** → la URL que falló.
- `pd.setProperty("errors", ...)` agrega campos extra al JSON estándar.

#### El frontend ya lo aprovecha (`app.js`)
```js
const msg = (data && (data.detail || data.message || data.error || data.mensaje)) || `Error ${res.status}`;
```
Se lee **`data.detail` primero** (el campo de RFC 7807), con los nombres antiguos como respaldo.

#### Para el examen
- Problem Details (RFC 7807) = formato JSON **estándar** para errores; en Spring = clase `ProblemDetail`.
- Se activa para los errores del framework con `spring.mvc.problem-details.enabled=true`.
- El campo `detail` lleva el mensaje de la excepción; `setProperty(...)` agrega campos extra.

---

### El flujo completo de un error (resumen del Bloque 3)
```
POST /api/carrito/items  con cantidad > stock
  └─ CarritoController.agregarItem()
       └─ carritoService.agregarItem()
            └─ stock insuficiente → throw new OperationException("Stock insuficiente...")  [SERVICIO: detecta el error de negocio]
  └─ GlobalExceptionHandler.handleOperation(OperationException)
       └─ return problem(400, "Solicitud inválida", "Stock insuficiente...")               [ADVICE: decide el HTTP]
  └─ Spring + problem-details
       └─ JSON 400 { "status":400, "detail":"Stock insuficiente...", ... }                 [SPRING: formatea estándar]
```
Tres capas, tres responsabilidades: **el servicio detecta**, **el `@RestControllerAdvice` traduce a HTTP**, **Spring formatea la respuesta**.

---

# MAPA DE CONCEPTOS PARA EL EXAMEN

| Tema | Anotación / Clase clave | Para qué sirve |
|---|---|---|
| **Webhook** | `@RequestHeader`, `@RequestBody String` | Recibir llamadas HTTP automáticas de otro sistema |
| **HMAC** | `HmacUtils`, `HMAC_SHA_256` | Verificar autenticidad+integridad con clave secreta |
| **Anti-replay** | `x-timestamp`, `Instant.now()` | Rechazar notificaciones viejas reenviadas |
| **Cliente HTTP** | `RestClient`, timeouts | Consumir una API externa enviando/recibiendo DTOs |
| **Idempotencia** | revisar estado antes de actuar | Procesar duplicados sin efectos secundarios |
| **Async con resultado** | `@Async`, `Future<T>`, `CompletableFuture` | Método async que devuelve un valor |
| **Esperar resultado** | `future.get()` | Bloquear hasta que el async termine |
| **Pool de hilos** | `@EnableAsync`, `ThreadPoolTaskExecutor` | Proveer los hilos/trabajadores de `@Async` |
| **Scatter-gather** | dos bucles (lanzar / recoger) | Ejecutar N tareas en paralelo y esperarlas |
| **Paginación** | `Page`, `Pageable`, `PageRequest`, `Sort` | Traer datos de a páginas; `.map()` a DTO |
| **Job** | `@Scheduled(cron=...)`, `@EnableScheduling` | Ejecutar código periódicamente (cron de 6 campos) |
| **Auditoría** | `@LastModifiedDate`, `@EnableJpaAuditing` | Fecha de modificación automática |
| **Excepción personalizada** | `extends RuntimeException` | Distinguir errores propios (`OperationException`, `NotDataFoundException`) |
| **Manejo central** | `@RestControllerAdvice`, `@ExceptionHandler` | Traducir excepciones a HTTP en un solo lugar |
| **Problem Details** | `ProblemDetail`, `spring.mvc.problem-details.enabled=true` | Formato JSON estándar (RFC 7807) para errores |

## Flujo mental para repasar (el barrido de carritos abandonados)
```
Cada 8 horas (reloj → @Scheduled)
  └─ barrerCarritosAbandonados()
       └─ repite por cada lote de carritos ACTIVO con fechaActualizacion vieja:
            ├─ PageRequest.of(0, 50, Sort by fechaActualizacion asc)   ← paginación (siempre pág. 0)
            ├─ por cada carrito: procesarCarritoAbandonado(id) ─→ Future ← scatter (hilos async-X en paralelo)
            ├─ por cada Future: future.get()                            ← gather (espera a todos)
            └─ siguiente lote, hasta que no queden (tope MAX_LOTES)
```
Si puedes narrar ese diagrama y explicar **por qué los dos bucles van separados** (paralelo vs secuencial) y **por qué aquí siempre se lee la página 0** (los carritos salen del filtro al marcarse ABANDONADO), dominas el corazón del Bloque 2. 💪

## Conexión con el parcial anterior
- **HMAC** aquí = la misma idea de firma del **JWT** del parcial 2 (clave secreta simétrica), aplicada al body del webhook en vez del token de login.
- **`Future`** aquí = la evolución del **`@Async void`** (ahora devuelve resultado y se puede esperar).
- **Paginación** aquí = la evolución de los **repositorios** del parcial 2 (ahora devuelven `Page` en vez de `List`).
- **`OperationException`/`NotDataFoundException` (unchecked)** se conectan con **transacciones**: al ser `RuntimeException`, disparan el **rollback automático** sin `rollbackFor`.
- **`@RestControllerAdvice`** complementa la **seguridad** del parcial 2: igual que el filtro JWT devuelve `401/403`, ahora el manejador global devuelve `400/404/500` con mensaje claro y formato estándar.
```
