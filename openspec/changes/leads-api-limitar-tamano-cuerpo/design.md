# Design

## Context

- `FiltroClaveApi` (`seguridad/filter`) corre con `Ordered.HIGHEST_PRECEDENCE`,
  nunca lee el cuerpo y delega el 401 en el `HandlerExceptionResolver`, así que
  el error sale del único `ManejadorErrores`.
- `ManejadorErrores` traduce las excepciones de Spring MVC al catálogo en
  `handleExceptionInternal`. Hoy un `HttpMessageNotReadableException` siempre
  es `json-invalido`.
- `TipoError.CUERPO_DEMASIADO_GRANDE` (413) ya existe y nadie lo usa.
- El cuerpo solo se lee en `POST /evaluar`, con `@RequestBody SolicitudCruda`
  (Jackson 3). Las demás rutas responden 404, 405 o 415 sin leerlo.
- El `session_id` del log de errores sale de un atributo del request que se
  guarda recién después de validar la solicitud (`SessionIdDelRequest`).
- Tomcat no limita el cuerpo de un JSON: `max-http-form-post-size` aplica a
  formularios y `max-swallow-size` (2 MB) al cuerpo que descarta después de
  responder.

Motivación y alcance: ver proposal.md. Comportamiento: ver la spec delta de
`acceso-api`.

## Endpoints

Sin endpoints nuevos ni cambios en el request o la respuesta exitosa.

| Ruta | Request | Response |
| --- | --- | --- |
| `POST /evaluar` | Igual que hoy, con cuerpo de 16384 bytes como máximo | 200 como hoy; 413 si el cuerpo supera el límite |
| Cualquier otra ruta o método | Cualquiera | Con Content-Length > 16384: 413. Si no, 404 / 405 / 415 como hoy |

Type de error que entra en uso: `urn:leads-api:error:cuerpo-demasiado-grande`
(413), sin `errores`. Los demás type no cambian. Reglas del DRL: sin cambios
(ni ELEGIBILIDAD ni PUNTAJE); `version_reglas` no cambia.

## Decisions

### D1. Filtro propio en `seguridad/filter`, después del de la clave

`FiltroTamanoCuerpo extends OncePerRequestFilter`, con
`@Order(Ordered.HIGHEST_PRECEDENCE + 1)`. Va junto a `FiltroClaveApi` porque
los dos protegen el servicio expuesto y su orden relativo es parte del
contrato: el de la clave primero (401 sin leer nada) y el de tamaño después.
El límite es una constante, `MAXIMO_BYTES = 16384`.

- *Alternativa:* `comun/filter`, como sugerían las notas técnicas. Se descarta
  para que los dos filtros de acceso y su orden se lean en un solo paquete.
- *Alternativa:* propiedades de Tomcat o del conector. Se descarta: ninguna
  limita el cuerpo de un JSON, y no cubrirían chunked de forma uniforme.
- *Alternativa:* hacerlo configurable. Descartada en el explore: el límite es
  fijo (ver proposal, Decisiones confirmadas).

### D2. Dos controles: declarado y leído

1. **Declarado:** si `request.getContentLengthLong() > MAXIMO_BYTES`, el
   filtro no sigue la cadena y delega una `CuerpoDemasiadoGrandeException` en
   el `HandlerExceptionResolver`, como `FiltroClaveApi`. Como corre antes que
   el `DispatcherServlet`, gana sobre 404, 405 y 415.
2. **Leído:** en cualquier otro caso, envuelve el request en un
   `HttpServletRequestWrapper` cuyo `getInputStream()` devuelve un
   `ServletInputStream` que cuenta los bytes leídos (`read()` y
   `read(byte[], int, int)`) y lanza `CuerpoDemasiadoGrandeException` cuando el
   total pasa de 16384. Cubre chunked, porque cuenta lo leído y no el header.
   `getReader()` también se arma sobre ese stream.

`CuerpoDemasiadoGrandeException` (`comun/exception`) sigue el estilo de
`NoAutorizadoException`: `RuntimeException` sin stack trace ni datos del
pedido.

- *Alternativa:* solo el contador, sin mirar Content-Length. Se descarta: una
  ruta que no lee el cuerpo nunca dispararía el límite, y la decisión
  confirmada es que el tamaño declarado da 413 en cualquier ruta.

### D3. 413 aunque Jackson envuelva la excepción

Cuando el contador corta durante la lectura, la excepción llega a Spring
envuelta (Jackson la convierte en su propia excepción y Spring en
`HttpMessageNotReadableException`). `ManejadorErrores` recorre la cadena de
causas: si encuentra una `CuerpoDemasiadoGrandeException`, responde
`CUERPO_DEMASIADO_GRANDE`; si no, `json-invalido` como hoy. Además tiene un
`@ExceptionHandler(CuerpoDemasiadoGrandeException.class)` para el caso
declarado, que llega sin envolver desde el filtro.

**Lectura del resto (agregado durante la implementación).** Jackson deja de
leer en cuanto el JSON falla o el objeto termina, así que con chunked el
contador solo no alcanza:
- un JSON roto en los primeros bytes de un cuerpo de 20 KB daría 400, y
- un JSON válido chico seguido de 20 KB de basura daría 200.

Por eso `FiltroTamanoCuerpo.leerElResto(request)` termina de leer lo que queda
del cuerpo, con el mismo contador, si alguien ya empezó a leerlo. Se usa en
dos lugares:
- en `ManejadorErrores`, ante un `HttpMessageNotReadableException` sin
  `CuerpoDemasiadoGrandeException` en la cadena: si al leer el resto se pasa
  el límite, 413; si no, `json-invalido`;
- en un `RequestBodyAdvice` (`LimiteCuerpoLeido`, `seguridad/config`), después
  de leer bien el cuerpo: si el resto pasa el límite, 413 antes de evaluar.

Una ruta que nunca lee el cuerpo no lo abre, así que sigue respondiendo 404
como pide la spec.

- *Alternativa:* leer el cuerpo completo en el filtro a un buffer y medirlo
  antes de pasarlo. Se descarta: una ruta inexistente con un cuerpo grande
  chunked daría 413 y no 404, contra la spec, y obliga a tener en memoria
  hasta 16 KB por pedido aunque nadie lo lea.
- *Alternativa:* aceptar que con chunked el 413 solo se garantiza cuando el
  JSON se lee entero. Se descarta: contradice el requirement («un cuerpo
  mayor MUST responder 413»).

### D4. El log no cambia de forma

El 413 pasa por el mismo `enviar(...)` que los demás errores: una línea
`error type=... status=413`. No lleva `session_id` porque el atributo se
guarda después de validar, y el cuerpo nunca se registra.

### D5. Tests con MockMvc y con un servidor real

- **MockMvc** para todo lo que tiene Content-Length: MockMvc lo arma con el
  largo del contenido, y los filtros registrados como beans corren igual que
  en producción. Bordes 16384 y 16385, orden frente a 401, 404, 405, 415 y
  400, forma del ProblemDetail y log.
- **Servidor real** (`@SpringBootTest(webEnvironment = RANDOM_PORT)`) con
  `java.net.http.HttpClient` y `BodyPublishers.ofInputStream(...)` para
  chunked: con un largo desconocido el cliente manda Transfer-Encoding chunked.
  El test verifica que el pedido no lleva Content-Length.
- Los cuerpos de los bordes se arman con la solicitud base más un campo extra
  de relleno (que se ignora), ajustado para llegar al tamaño exacto en bytes
  UTF-8.

## Risks / Trade-offs

- [Con chunked, si el cliente sigue mandando después del 413 y el resto
  supera los 2 MB de `max-swallow-size`, Tomcat cierra la conexión y el
  cliente puede ver un corte en vez del 413] → Aceptable: es justamente el
  caso abusivo; n8n manda unos 300 bytes con Content-Length.
- [Un Content-Length falso (menor que el cuerpo real)] → Tomcat lee solo los
  bytes declarados, así que el cuerpo efectivo nunca pasa del límite; el
  contador lo cubre igual.
- [El filtro corre en toda petición] → Costo despreciable: una comparación y
  un wrapper.
- [El cuerpo se cuenta en bytes, no en caracteres] → Es lo buscado: el límite
  es de transporte. Un texto con tildes ocupa más bytes y los tests de borde
  lo arman en bytes UTF-8.

## Migration Plan

Sin cambios en n8n ni en la configuración. Después del merge, reiniciar el
servicio. Rollback: revertir el merge; el contrato exitoso no cambia.
