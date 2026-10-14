# Proposal

## Change ID

`leads-api-limitar-tamano-cuerpo`

## Why

El servicio está expuesto a internet por ngrok y hoy lee el cuerpo de cualquier
pedido sin importar su tamaño. Un pedido normal de n8n pesa unos 300 bytes,
pero con la clave alguien podría mandar cientos de megabytes y el servicio
intentaría leerlos y procesarlos. Este change agrega el límite de 16 KB previsto
en el plan original (decisiones 18 y 20). Era el último riesgo abierto para
dejar el servicio expuesto, que ahora además recibe pedidos desde Telegram y el
chat web.

## What Changes

- Límite de **16384 bytes** para el cuerpo de cualquier pedido, contados sobre
  el cuerpo tal como llega, sin los headers.
- Si el pedido **declara** su tamaño (Content-Length) y supera el límite,
  responde 413 con el error `cuerpo-demasiado-grande` apenas llega, sin leer el
  cuerpo, a cualquier ruta y con cualquier método: antes que 404, 405 y 415.
- Si el pedido **no declara** su tamaño (Transfer-Encoding chunked), el límite
  se controla mientras se lee: al pasar los 16384 bytes, 413. Solo `POST
  /evaluar` lee el cuerpo, así que a una ruta inexistente sigue respondiendo
  404.
- Sin clave, 401 aunque el cuerpo sea grande: el filtro de la clave corre
  primero y nunca lee el cuerpo.
- Un cuerpo de 16384 bytes o menos sigue el camino de siempre (200, 400, etc.).
- El error `cuerpo-demasiado-grande` ya está en el catálogo desde el change 1;
  este change es el primero que lo usa.

## Contratos afectados

**Todas las rutas, incluido POST /evaluar** (compatible con n8n):

| Campo o respuesta | Dirección | Cambio |
| --- | --- | --- |
| Cuerpo del request | request | Nuevo máximo de 16384 bytes. Por encima: 413 |
| 413 `cuerpo-demasiado-grande` | response | Respuesta nueva en la práctica: ProblemDetail con `type` `urn:leads-api:error:cuerpo-demasiado-grande` y `status` 413, sin `errores` |

No entran ni salen campos del request ni de la respuesta exitosa. n8n no
necesita cambios: sus pedidos pesan unos 300 bytes, más de 50 veces menos que
el límite. Si alguna vez recibiera un 413, el flujo lo guarda como `ERROR` con
su `type`, como cualquier otro error del servicio.

## Capabilities

### New Capabilities

Ninguna.

### Modified Capabilities

- `acceso-api`: nuevo requirement «Tamaño máximo del cuerpo», con su orden
  frente a 401, 404, 405, 415 y 400; y el escenario «Cada type usado está en el
  catálogo» de «Catálogo de type de error» pasa a incluir el 413.

`evaluacion-leads` no cambia.

## Decisiones confirmadas

De la especificación inicial (decisiones 18 y 20, 2026-10-09):
- Límite de 16 KB para el cuerpo, que también cubre Transfer-Encoding chunked,
  con 413 y su `type`.
- Orden de los errores: 401, después 413, después 400.

Del explore del change 4 (confirmadas por Julian, 2026-10-13):
- **El límite aplica a cualquier pedido**, no solo a `POST /evaluar`. Con
  Content-Length mayor al límite, 413 sin leer el cuerpo, antes que 404, 405 y
  415. Sin Content-Length, se corta al leer.
- **Borde exacto:** 16 KB son 16384 bytes. 16384 pasan; 16385 dan 413.
- **Límite fijo en el código**, no configurable: es un límite de seguridad, no
  una regla de negocio. Cambiarlo es un cambio de contrato y pasa por un
  change.

## Supuestos pendientes

- **Log del 413:** deja su línea de error con `type` y estado, sin
  `session_id`, porque el cuerpo no se llegó a interpretar. Es lo que ya dice
  «Registro de errores»; se agrega un escenario que lo verifica.
- **Estructura de la spec:** todo va en `acceso-api`, con un requirement nuevo
  y un `MODIFIED` del catálogo.
- **Documentación:** se suma el 413 a "Cómo leer la planilla" en
  `docs/integracion-n8n.md`, y se levanta la advertencia sobre el tamaño del
  cuerpo en esa guía y en el roadmap. Se mantiene la recomendación de cortar
  ngrok cuando no se usa, porque el servicio depende de una PC.

## Fuera de alcance

- El largo máximo de un número dentro del JSON y el formato estricto de los
  números (change 3).
- Límites de cantidad de pedidos por minuto o por cliente.
- Límites sobre los headers o la URL: los aplica Tomcat con sus valores por
  defecto.
- Cambios en n8n, en Telegram o en el chat web.

## Estrategia de testing

- **Bordes con Content-Length:** cuerpo de exactamente 16384 bytes (una
  solicitud base válida rellenada con un campo extra, que se ignora): 200;
  16385 bytes: 413.
- **Chunked:** con un servidor real (puerto aleatorio) y un cliente HTTP que
  envía el cuerpo sin Content-Length: 20 KB dan 413 y la solicitud base da 200.
  El test verifica que el pedido no lleva Content-Length.
- **Orden:** 20 KB sin clave dan 401; 20 KB con clave y JSON inválido dan 413,
  no 400; 20 KB declarados a una ruta inexistente, con GET /evaluar y con
  Content-Type `text/plain` dan 413; 20 KB chunked a una ruta inexistente dan
  404.
- **Forma de la respuesta:** ProblemDetail con
  `application/problem+json;charset=UTF-8`, `type` y `status` 413, sin
  `errores`.
- **Log:** una línea con el `type` y `413`, sin `session_id` ni contenido del
  cuerpo.
- **Regresión:** toda la suite existente sigue en verde; los cuerpos actuales
  están muy por debajo del límite.

## Aprobadores requeridos

- Julian: aprueba la propuesta con un comentario en el PR y revisa el código.

## Impact

- Código: un filtro nuevo en `comun` (después del de la clave), el manejo del
  413 en `ManejadorErrores` (incluido cuando la excepción llega envuelta por
  Jackson) y tests nuevos.
- Sin dependencias nuevas ni cambios de configuración.
- Docs: `docs/integracion-n8n.md`, `docs/roadmap.md` y
  `docs/notas-tecnicas-leads.md` (puntos b y d).
