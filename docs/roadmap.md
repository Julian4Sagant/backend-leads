# Roadmap de leads-api

Plan corto de changes para construir leads-api, el servicio sin estado que
recibe una solicitud de préstamo ya normalizada por n8n y devuelve la prioridad
calculada por el motor de reglas. Surge del explore inicial (2026-10-09) y se
actualizó con el explore del change 1 (2026-10-13), que eligió Drools como
motor de reglas. Las verificaciones técnicas están en
[notas-tecnicas-leads.md](notas-tecnicas-leads.md).

Estado: change 1 archivado (`openspec/changes/archive/2026-10-13-leads-api-crear-evaluacion-puntaje`).
`openspec/specs/` tiene `acceso-api` y `evaluacion-leads`.

## Capabilities

| Capability | Qué cubre la spec |
| --- | --- |
| `evaluacion-leads` | Contrato de POST /evaluar (request, response, validación de campos, formatos y rangos); reglas excluyentes y de puntaje; prioridad; score con recortes y ajuste por inelegibilidad; motivos y su orden; tabla de máximos por situación laboral; `version_reglas` |
| `acceso-api` | Clave X-API-Key (401, arranque sin clave, comparación en tiempo constante); sin actuator ni springdoc; 404 y 405 con clave; límite de 16 KB (413); orden 401 > 413 > 400; formato de errores ProblemDetail y su tabla de `type` |

El formato de errores vive en `acceso-api` para que la tabla de `type` esté en
un solo lugar. `evaluacion-leads` la referencia.

## Changes

```
  +-------------------------------------------+
  | 1. leads-api-crear-evaluacion-puntaje     |
  +-------------------------------------------+
        |                 |                |
        v                 v                v
  +-------------+  +---------------+  +-------------+
  | 2. agregar- |  | 3. endurecer- |  | 4. limitar- |
  | elegibilidad|  | formato-      |  | tamano-     |
  | -edad       |  | numerico      |  | cuerpo      |
  +-------------+  +---------------+  +-------------+

  Orden recomendado: 1, 2, 3, 4.  2, 3 y 4 dependen solo de 1.
```

### 1. `leads-api-crear-evaluacion-puntaje` (archivado)
Rebanada base: POST /evaluar de punta a punta.
- Clave de API con un filtro propio, sin Spring Security (401; la app no
  arranca sin `LEADS_API_KEY` o con una clave de menos de 32 caracteres, con
  caracteres no ASCII imprimibles o con espacios), sin actuator ni springdoc,
  404/405 con clave válida.
- ProblemDetail desde un único `@RestControllerAdvice`, con la tabla de `type`
  y `charset=UTF-8` explícito. Solo acepta `Content-Type: application/json`
  (si no, 415) e ignora el header Accept: responde siempre JSON y nunca 406.
- Drools 10.2.0 como motor de reglas (ver "Motor de reglas").
- Se saca lombok del `pom.xml`.
- Contrato nuevo. **Cambio incompatible para n8n**, a declarar en "Contratos
  afectados": sale `nombre` del request; entran `edad` y `antiguedad_meses`
  (requeridos); entra `version_reglas` en la response.
- Validación: requeridos, valores permitidos de cada código, rangos con su
  borde exacto (decisión 15, que pasó del change 3 a este), todos los errores
  juntos, campos extra ignorados.
- Las 7 reglas de puntaje en el DRL (motivo `PUNTAJE`), prioridad, recorte
  0/100 con su motivo, orden de motivos, carga con BigDecimal sin dividir.
  En este change la respuesta tiene 7 motivos, u 8 con recorte.
- Puntajes y umbrales en `application.yml` con validación de coherencia;
  `version_reglas` y sus tests; tabla de máximos y su test; UTF-8.
- Decisiones: 1, 2, 4 a 12, 14 a 17, 21 a 24.
- El DTO usa desde el inicio la representación "cruda" de los números (notas,
  punto c), para que el change 3 no tenga que rehacerlo.

### 2. `leads-api-agregar-elegibilidad-edad` (archivado)
Decisiones del explore del 2026-10-13:
- Regla de edad en el DRL (motivo `ELEGIBILIDAD`). Menor que la edad mínima:
  no elegible, prioridad BAJA, score 0. Las reglas de puntaje se evalúan igual
  y aparecen en los motivos.
- Edad mínima configurable en `application.yml` (hoy 18), como el resto de los
  umbrales. Los textos usan el valor configurado: "No elegible: menor de 18
  años" y "Edad de 18 años o más: elegible" (este último reemplaza al
  "Mayor de edad: elegible" del plan original, que quedaría engañoso con otra
  edad mínima).
- Precedencia del ajuste por inelegibilidad sobre el recorte. El motivo final
  "Score ajustado a 0 por inelegibilidad (suma X)" aparece **siempre** que la
  solicitud es inelegible, también con suma 0 o entre 0 y 100.
- **Cambio de rango de `edad`: de 14 a 100 pasa a 1 a 100**, para que un menor
  de 14 figure como no elegible y no como error. Todos los menores llevan el
  mismo mensaje. 0 o menos y más de 100 siguen siendo `fuera-de-rango`. Es
  compatible con n8n: se aceptan más valores, nunca menos. Modifica el
  requirement «Rangos de los números» de `evaluacion-leads` y la decisión 14.
- Bordes: edad 0, 1, 17, 18, 100 y 101.
- Decisión: 3. Depende de 1.
- Modifica con un `MODIFIED` el requirement «Orden y forma de los motivos» de
  `evaluacion-leads`: el motivo excluyente va al principio, así que la
  respuesta pasa a tener 8 motivos (9 con recorte o inelegibilidad). Actualiza
  los tests del orden y del conteo.
- Implementado el 2026-10-13 (PR #4). Durante la implementación también se
  modificó «Campos requeridos»: el escenario «Varios errores en una misma
  solicitud» usaba `edad` 13 como dato inválido y pasó a usar `edad` 0.
  Confirmado: un menor de 18 figura como no elegible, nunca como error.
- `version_reglas` cambia para todos los leads (`REVISION_LOGICA` 2).
- Después del merge hay que reiniciar el servicio para que tome las reglas
  nuevas. n8n no necesita cambios.

### 3. `leads-api-endurecer-formato-numerico`
- Una misma regex por campo para número JSON y para string numérico.
- Todos los escenarios 400 de la decisión 13, incluidos "" y espacios. Hoy
  `1e3` y `+5` se aceptan, y `.5` también en importes (validación con el
  formato de BigDecimal).
- Mensajes distintos para formato inválido y tipo incorrecto (los códigos ya
  se distinguen desde el change 1).
- Longitud máxima de números en el parser
  (`spring.jackson.factory.constraints.read.max-number-length`; hoy rige el
  default de Jackson, 1000 dígitos).
- Decisiones: 13 y 19. Depende de 1. Los bordes de rango (decisión 15) ya
  están en el change 1.

### 4. `leads-api-limitar-tamano-cuerpo` (en curso)
- Filtro propio de 16 KB que cubre Transfer-Encoding chunked; 413 con su `type`.
- Orden 401 > 413 > 400.
- Decisiones: 18 y 20. Depende de 1.

Decisiones del explore del 2026-10-13:
- **El límite aplica a cualquier pedido.** Si el Content-Length declara más
  de 16 KB, se responde 413 apenas llega, sin leer el cuerpo, a cualquier ruta
  y con cualquier método: antes que 404, 405 y 415. Si no declara tamaño
  (chunked), se corta al leer; como solo `POST /evaluar` lee el cuerpo, una
  ruta inexistente sigue dando 404.
- **Borde exacto:** 16 KB son 16384 bytes del cuerpo, sin los headers. 16384
  pasan y 16385 dan 413.
- **Límite fijo en el código**, no configurable: es un límite de seguridad,
  no una regla de negocio. Cambiarlo es un cambio de contrato y pasa por un
  change.
- Sin clave, 401 aunque el cuerpo sea grande: el filtro de la clave corre
  primero y nunca lee el cuerpo.
- El 413 deja su línea de error con `type` y estado, sin `session_id` (el
  cuerpo no se llegó a interpretar), como dice «Registro de errores».
- Spec: solo `acceso-api`. Un requirement nuevo, «Tamaño máximo del cuerpo»,
  y un `MODIFIED` de «Catálogo de type de error» para sumar el 413 al
  escenario «Cada type usado está en el catálogo». `evaluacion-leads` no
  cambia.
- n8n no cambia: un 413 quedaría como `ERROR` con su `type`. Se suma esa
  fila a "Cómo leer la planilla" en la guía de integración, y al cerrar el
  change se levanta la advertencia sobre el tamaño del cuerpo en el roadmap y
  en la guía.
- Fuera de alcance: el largo máximo de un número (change 3) y cualquier
  límite de cantidad de pedidos.

**Riesgo de orden:** entre el change 1 y el 4 el servicio no limita el tamaño
del cuerpo. Si se expone por ngrok antes de cerrar el 4, el riesgo queda
abierto. Hay dos opciones: no exponerlo hasta el 4, o adelantar el 4 al segundo
lugar (es independiente de 2 y 3).

## Decisiones transversales (confirmadas en el explore del change 1)

### Motor de reglas
Drools 10.2.0, verificado con un spike (notas técnicas, "Spike de Drools").

```
 POST /evaluar -> filtro de clave -> validación -> Solicitud tipada
                                                       |
                                                       v
                  StatelessKieSession (una por solicitud)
                  hechos: Solicitud, Resultado; global: puntajes
                  cada regla emite un código de motivo con su posición
                                                       |
                                                       v
                  Java: textos, orden por posición, suma, recorte o
                  inelegibilidad, score, prioridad
```

- Cada regla de negocio es un grupo de reglas DRL mutuamente excluyentes, una
  por tramo, que comparten un identificador estable (`@regla`). Exactamente
  una de cada grupo dispara. Excluyentes y de puntaje se distinguen por el
  tipo de motivo que emiten (`ELEGIBILIDAD` o `PUNTAJE`), no por interfaces de
  Java.
- El DRL emite códigos y es solo ASCII. Los textos en español se arman en Java,
  con los umbrales configurados ("hasta el 30%", "12 meses o más"), así un
  motivo nunca describe un corte distinto del real.
- Los puntajes y umbrales siguen en `application.yml` y llegan al DRL como
  global. El DRL no lleva números de negocio, ni siquiera en los nombres: los
  tramos se llaman `baja`/`media`/`alta`.
- El DRL se compila al arrancar. Si tiene errores, la app no arranca.
- `version_reglas` = SHA-256 de (configuración canónica + DRL con finales de
  línea LF + constante de revisión), truncado a 12 caracteres hexadecimales.
  La constante se sube a mano cuando cambia la lógica en Java o un texto. El
  test de cierre vigila la lista de reglas del `KieBase` compilado.

### Formato de errores
ProblemDetail (RFC 9457), `application/problem+json;charset=UTF-8`, desde un
único `@RestControllerAdvice`. Los filtros (clave y tamaño) no escriben la
respuesta por su cuenta: delegan en `HandlerExceptionResolver`, así el advice
sigue siendo el único lugar que arma errores.

Los `type` son URN `urn:leads-api:error:<caso>`, en minúsculas, ASCII y con
guion. Uno por caso, estable y registrado en la spec. Un change que necesita un
`type` nuevo lo agrega a la spec en ese change. Nunca se usa `about:blank`.

| Estado | `type` | Cuándo |
| --- | --- | --- |
| 400 | `urn:leads-api:error:validacion` | Campos con errores. Siempre trae al menos un error en `errores: [{campo, codigo, mensaje}]` |
| 400 | `urn:leads-api:error:json-invalido` | Cuerpo que no es JSON válido, texto suelto, cuerpo vacío, o número que excede la longitud máxima del parser. Nunca trae lista de errores |
| 401 | `urn:leads-api:error:no-autorizado` | Sin X-API-Key o con una clave incorrecta |
| 404 | `urn:leads-api:error:no-encontrado` | Ruta desconocida (con clave válida) |
| 405 | `urn:leads-api:error:metodo-no-permitido` | Método no soportado (con clave válida) |
| 413 | `urn:leads-api:error:cuerpo-demasiado-grande` | Cuerpo de más de 16 KB (comportamiento en el change 4) |
| 415 | `urn:leads-api:error:tipo-de-contenido-no-soportado` | Content-Type ausente o distinto de `application/json` |
| 500 | `urn:leads-api:error:interno` | Error no previsto, sin detalles internos en la respuesta |

El header Accept se ignora: el servicio responde siempre JSON y nunca 406.

`{}` es JSON válido: va a `validacion`, con un error `requerido` por campo.

Códigos de `errores[].codigo`: `requerido`, `tipo-invalido`, `formato-invalido`,
`valor-no-permitido`, `fuera-de-rango`. El guion es solo para identificadores
de error. Los códigos de dominio (`esta_semana`, `1_a_3_meses`, `no_se`) y los
nombres de campo (`session_id`, `plazo_meses`) siguen con guion bajo porque son
el contrato con n8n y el Sheet.

### Paquetes
Primero por funcionalidad y después por tipo de clase, bajo
`com.sagant.leads.leads_api` (detalle en el design del change 1, D12):

```
evaluacion/  controller · dto · enums · model · validation · service · config
seguridad/   config · filter · service
comun/       config · enums · exception · handler
```

Los tests siguen la misma estructura. El DRL vive en
`src/main/resources/reglas/`. El filtro de tamaño del cuerpo (change 4) va en
`comun/filter/`.

### Logging
- Una línea por evaluación exitosa con `session_id`, prioridad, score y
  `version_reglas`.
- Una línea por respuesta de error con el `type` y el estado, más el
  `session_id` si es válido. En `json-invalido`, 401 y 413 la línea va sin
  `session_id`.
- `session_id` se loguea solo si cumple `^[A-Za-z0-9_-]{1,64}$`, y nunca junto
  a ningún otro campo de la solicitud.
- Nunca se loguean el cuerpo, los valores recibidos, los mensajes de los
  errores de campo, el header X-API-Key ni los campos extra.
- Los ejemplos y fixtures de test usan datos ficticios.

### Configuración
- `leads.reglas.*`: `@ConfigurationProperties` con `@Validated`. Umbrales y
  puntajes con BigDecimal donde corresponde. La coherencia (ALTA > MEDIA,
  0,3 < 0,5, etc.) se valida al arrancar.
- `leads.seguridad.api-key: ${LEADS_API_KEY}`: mínimo 32 caracteres ASCII
  imprimibles y sin espacios. Si no cumple, la app no arranca y el mensaje no
  muestra la clave. Sugerencia para generarla: `openssl rand -base64 32`. Los
  tests usan una clave ficticia.
- `spring.jackson.factory.constraints.read.max-number-length` (ver notas,
  punto a): se configura en el change 3.
- En `application.yml`, la clave `no` va entre comillas (`"no": 0`): YAML la
  lee como el booleano `false` si no.

### Comportamiento de negocio confirmado
Con los puntajes actuales (tabla en las notas técnicas, punto f):
- con `clearing = si` ninguna solicitud llega a ALTA (máximo 55);
- un estudiante llega a ALTA solo con el puntaje perfecto (60, justo en el
  borde);
- un desempleado nunca llega a ALTA (máximo 50).

Desde el change 2, estos máximos valen para solicitudes elegibles (18 años o
más). Un menor de la edad mínima siempre queda con score 0 y prioridad BAJA.

## Preguntas abiertas por change

### Change 1
Ninguna: todas se cerraron en el explore del 2026-10-13.

### Change 2
Ninguna: todas se cerraron en el explore del 2026-10-13 (ver el change 2).

### Change 3
- Claves duplicadas en el JSON: ¿400 o gana la última? Se puede activar
  `spring.jackson.read.strict-duplicate-detection`. Hoy gana la última.
- Un string numérico negativo ("-5"): ¿se informa como `formato-invalido` o
  como `fuera-de-rango`? Hoy da `fuera-de-rango`.
- ¿Un entero escrito con decimales en cero (`12.0`) es válido? Hoy da
  `formato-invalido`: la regla mira cómo está escrito, no su valor.

### Change 4
Ninguna: todas se cerraron en el explore del 2026-10-13 (ver el change 4).

## Pendientes fuera del plan

La operación actual (servicio en una PC, ngrok y el flujo de n8n) está en
[integracion-n8n.md](integracion-n8n.md).

Surgieron después del change 1 y todavía no tienen change ni decisión:
- **Que el servicio no dependa de una PC.** Hoy corre en una computadora local
  expuesta con ngrok: si se apaga o se corta ngrok, los leads quedan en
  `ERROR` sin calificar. Para usarlo con clientes reales hace falta un
  servidor siempre disponible.
- **Qué hacer con los leads en `ERROR`.** Hoy quedan marcados en la planilla
  y nadie los vuelve a calificar: falta decidir si se revisan a mano o se
  reintentan.
- **Avisar en el chat a un menor de edad desde el principio** (en n8n), en vez
  de hacerle responder las 12 preguntas para terminar no elegible.

## Próximo paso
Propuesta del change 4 (`/opsx:propose leads-api-limitar-tamano-cuerpo`).
Después, el change 3 (formato estricto de los números).
